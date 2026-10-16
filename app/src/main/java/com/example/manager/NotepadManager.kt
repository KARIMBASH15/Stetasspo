package com.example.manager

import android.content.Context
import com.example.model.NoteFolder
import com.example.model.NoteItem
import com.example.model.NoteType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class NotepadManager(private val context: Context) {

    private val notesFile = File(context.filesDir, "notepad_vault.json")

    private val _folders = MutableStateFlow<List<NoteFolder>>(emptyList())
    val folders: StateFlow<List<NoteFolder>> = _folders.asStateFlow()

    private val _notes = MutableStateFlow<List<NoteItem>>(emptyList())
    val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

    init {
        initDefaultData()
    }

    private fun initDefaultData() {
        val defaultFolders = listOf(
            NoteFolder("f_clipboard", "Quick Clipboard", "الحافظة السريعة", 0xFF38BDF8),
            NoteFolder("f_links", "Saved Links & URLs", "روابط ومواقع", 0xFF818CF8),
            NoteFolder("f_vault", "Secrets & Passwords", "خزنة الكلمات والسر", 0xFFF59E0B),
            NoteFolder("f_code", "Code & HTML Snippets", "أكواد وبرمجة", 0xFF10B981),
            NoteFolder("f_general", "Personal Notes", "مذكرات شخصية", 0xFFF43F5E)
        )
        _folders.value = defaultFolders

        if (notesFile.exists()) {
            loadNotesFromFile()
        } else {
            val sampleNotes = listOf(
                NoteItem(
                    id = "n1",
                    folderId = "f_links",
                    title = "YouTube Web Player",
                    content = "https://m.youtube.com",
                    type = NoteType.LINK,
                    isPinned = true
                ),
                NoteItem(
                    id = "n2",
                    folderId = "f_clipboard",
                    title = "ترحيب في المذكرة العائمة",
                    content = "يمكنك لصق أي نص أو رابط هنا بضغطة زر واحدة واستخدامه كخزينة للملاحظات والروابط السريعة!",
                    type = NoteType.CLIPBOARD_SNIPPET,
                    isPinned = true
                ),
                NoteItem(
                    id = "n3",
                    folderId = "f_code",
                    title = "HTML5 Boilerplate",
                    content = "<!DOCTYPE html>\n<html>\n<head><title>FileHub</title></head>\n<body><h1>Hello</h1></body>\n</html>",
                    type = NoteType.CODE
                )
            )
            _notes.value = sampleNotes
            saveNotesToFile()
        }
    }

    private fun loadNotesFromFile() {
        try {
            val jsonStr = notesFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<NoteItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    NoteItem(
                        id = obj.getString("id"),
                        folderId = obj.getString("folderId"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        type = NoteType.valueOf(obj.optString("type", NoteType.TEXT.name)),
                        dateModified = obj.getLong("dateModified"),
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                )
            }
            _notes.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveNotesToFile() {
        try {
            val array = JSONArray()
            _notes.value.forEach { note ->
                val obj = JSONObject().apply {
                    put("id", note.id)
                    put("folderId", note.folderId)
                    put("title", note.title)
                    put("content", note.content)
                    put("type", note.type.name)
                    put("dateModified", note.dateModified)
                    put("isPinned", note.isPinned)
                }
                array.put(obj)
            }
            notesFile.writeText(array.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addNote(folderId: String, title: String, content: String, type: NoteType = NoteType.TEXT) {
        val detectedType = if (content.trim().startsWith("http://") || content.trim().startsWith("https://")) {
            NoteType.LINK
        } else type

        val newNote = NoteItem(
            id = UUID.randomUUID().toString(),
            folderId = folderId,
            title = title.ifBlank { if (detectedType == NoteType.LINK) "Saved Link" else "Quick Note" },
            content = content,
            type = detectedType,
            dateModified = System.currentTimeMillis()
        )
        _notes.value = listOf(newNote) + _notes.value
        saveNotesToFile()
    }

    fun createFolder(name: String, nameAr: String, colorHex: Long) {
        val newFolder = NoteFolder(
            id = "f_${UUID.randomUUID().toString().take(6)}",
            name = name,
            nameAr = nameAr,
            colorHex = colorHex
        )
        _folders.value = _folders.value + newFolder
    }

    fun deleteNote(id: String) {
        _notes.value = _notes.value.filter { it.id != id }
        saveNotesToFile()
    }

    fun togglePin(id: String) {
        _notes.value = _notes.value.map {
            if (it.id == id) it.copy(isPinned = !it.isPinned) else it
        }
        saveNotesToFile()
    }

    fun getNotesForFolder(folderId: String?): List<NoteItem> {
        val current = _notes.value
        val filtered = if (folderId == null) current else current.filter { it.folderId == folderId }
        return filtered.sortedWith(compareByDescending<NoteItem> { it.isPinned }.thenByDescending { it.dateModified })
    }
}
