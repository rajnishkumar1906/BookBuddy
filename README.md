# 📚 BookBuddy - AI Library Management App

A modern Android app for digital library operations with role-based access, smart semantic search, and AI-assisted book interactions.

---

## 🚀 Features

### 👩‍💼 For Librarians
- 📊 **Dashboard Insights** - Live counters for books and members
- 📁 **CSV Bulk Upload** - Import large book datasets from CSV
- 🧠 **Auto Embeddings** - Generate vector embeddings while uploading
- 📄 **Sample CSV Download** - Export ready-to-use sample template

### 👨‍🎓 For Members
- 🔍 **AI Search** - Natural-language search over your catalog
- 📚 **Browse + Detail View** - Rich book cards and detailed pages
- 📥 **Borrow Book Flow** - 14-day due-date borrow records
- ⭐ **Book Ratings** - Submit ratings and update average score

### 🤖 AI Layer
- 🧭 **Semantic Matching** with cosine similarity
- ✨ **AI Summaries** using Hugging Face + Gemini fallback
- 🧩 **Context-Aware Q&A** (RAG-style chunk retrieval)
- 📝 **Auto Description Expansion** for short descriptions

---

## 📱 App Screens (Suggested)

| Librarian Dashboard | CSV Upload Dialog | Member Dashboard |
| --- | --- | --- |
| `assets/screens/librarian-dashboard.png` | `assets/screens/csv-upload.png` | `assets/screens/member-dashboard.png` |

| AI Search | Book Detail | AI Chat Mode |
| --- | --- | --- |
| `assets/screens/ai-search.png` | `assets/screens/book-detail.png` | `assets/screens/book-chat.png` |

> Add real screenshots in an `assets/screens/` folder and update these paths.

---

## 🛠️ Tech Stack

### Android
- **Language:** Kotlin
- **UI:** XML + Material Components
- **Min SDK:** 24
- **Compile SDK:** 36
- **Architecture:** Activity/Fragment modular flow

### Backend
- **Authentication:** Firebase Auth
- **Database:** Firebase Realtime Database

### AI + Networking
- **Embeddings & LLM APIs:** Hugging Face Inference
- **Summaries & Expansion:** Gemini API
- **HTTP Client:** OkHttp
- **JSON:** Gson
- **Async:** Coroutines
- **Image Loading:** Glide
- **CSV Parser:** OpenCSV

---

## 📁 Project Structure

```text
app/src/main/java/com/rajnishkumar/bookbuddy/
│
├── AuthActivity.kt
├── SplashActivity.kt
├── LoginFragment.kt
├── SignupFragment.kt
├── LibrarianDashboard.kt
├── MemberDashboard.kt
├── AISearchActivity.kt
├── BookDetailActivity.kt
├── Constants.kt
│
├── ai/
│   ├── AISearchHelper.kt
│   ├── BulkUploadHelper.kt
│   ├── HuggingFaceClient.kt
│   └── GeminiClient.kt
│
└── models/
    ├── Book.kt
    └── BorrowRecord.kt
```

---

## 🗄️ Database Schema (Realtime DB)

### `users/{userId}`
```json
{
  "id": "uid_123",
  "name": "Rajnish Kumar",
  "email": "user@example.com",
  "role": "member"
}
```

### `books/{bookId}`
```json
{
  "id": "book_123",
  "title": "Atomic Habits",
  "author": "James Clear",
  "genre": "Self-Help, Productivity",
  "genreList": ["Self-Help", "Productivity"],
  "description": "...",
  "summary": "...",
  "isbn": "9780735211292",
  "coverUrl": "https://...",
  "totalCopies": 1,
  "availableCopies": 1,
  "embedding": [0.01, -0.05, "..."],
  "averageRating": 4.6,
  "totalRatings": 89,
  "addedAt": 1700000000000,
  "addedBy": "uid_123"
}
```

### `borrowRecords/{recordId}`
```json
{
  "id": "br_123",
  "bookId": "book_123",
  "userId": "uid_456",
  "bookTitle": "Atomic Habits",
  "bookAuthor": "James Clear",
  "bookCoverUrl": "https://...",
  "borrowDate": 1700000000000,
  "dueDate": 1701209600000,
  "status": "BORROWED"
}
```

---

## 🔧 Setup Guide

### 1) Prerequisites
- Android Studio (latest stable)
- JDK 11+
- Android SDK 24+
- Firebase Project

### 2) Clone
```bash
git clone https://github.com/rajnishkumar1906/BookBuddy.git
cd BookBuddy
```

### 3) Firebase setup
1. Create project in Firebase Console  
2. Add Android app package: `com.rajnishkumar.bookbuddy`  
3. Download `google-services.json` into `app/`  
4. Enable Email/Password auth  
5. Enable Realtime Database  

### 4) API keys
Use secure local config (recommended) and avoid hardcoding tokens.

### 5) Build
```bash
./gradlew build
./gradlew installDebug
```

---

## 📥 CSV Import Format

The current parser reads columns in this order:
1. ISBN
2. Title
3. Author
4. Genres
5. Description
6. (unused)
7. Cover URL

### Sample row
```csv
9780141439518,Pride and Prejudice,Jane Austen,Classic,Love and social expectations,,https://example.com/cover.jpg
```

---

## 🎯 AI Search Workflow

1. Convert user query to embedding  
2. Compare against stored book embeddings  
3. Rank by cosine similarity  
4. Return top matching books in UI  

---

## 🔐 Security Notes

- Never commit real API keys/tokens in source code
- Restrict Firebase rules before production
- Use role checks for librarian-only operations

---

## 🧪 Current Build Status

### Implemented
- Auth + role routing
- Librarian/member dashboards
- CSV import with embeddings
- AI search + detail screen
- Borrow + rating flow
- AI summary and chat features

### Planned / Placeholder UI
- Add Book
- Manage Books
- Full Stats
- My Books screen
- Profile screen

---

## 🤝 Contributing

1. Fork the repo  
2. Create feature branch  
3. Commit changes  
4. Push branch  
5. Open PR  

---

## 👤 Author

- **Rajnish Kumar**
- GitHub: [rajnishkumar1906](https://github.com/rajnishkumar1906)

---

Made with care for readers, librarians, and builders.
