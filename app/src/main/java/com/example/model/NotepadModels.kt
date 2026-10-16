package com.example.model

enum class NoteType(val labelEn: String, val labelAr: String) {
    TEXT("Note", "ملاحظة"),
    LINK("Web Link", "رابط ويب"),
    CLIPBOARD_SNIPPET("Clipboard", "نص منسوخ"),
    CODE("Code", "كود برمجي")
}

data class NoteFolder(
    val id: String,
    val name: String,
    val nameAr: String,
    val colorHex: Long = 0xFF38BDF8
)

data class NoteItem(
    val id: String,
    val folderId: String,
    val title: String,
    val content: String,
    val type: NoteType = NoteType.TEXT,
    val dateModified: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
) {
    val isUrl: Boolean
        get() = content.trim().startsWith("http://") || content.trim().startsWith("https://") || content.trim().startsWith("www.")
}
