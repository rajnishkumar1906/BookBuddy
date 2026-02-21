# 📚 BookBuddy

**BookBuddy** is a local, explainable AI-powered virtual librarian.  
It helps users discover books, understand recommendations, and explore reading material using semantic understanding instead of keyword search.

This project demonstrates practical **ML engineering**, **RAG-based system design**, and **full-stack integration**, making it suitable for portfolio showcase.

---

## 🚀 Features

### 📖 Book Discovery
- Natural language book queries
- Semantic search across titles, authors, genres, and descriptions
- No keyword-based matching

### 🧠 Intelligent Recommendations
- Context-aware book recommendations
- Clear explanations for *why* a book is suggested
- Reasoning grounded in retrieved data

### 📝 Book Summaries
- Librarian-style summaries
- Minimum 50-word explanations
- Neutral and informative tone

### 🖼️ Text Extraction from Images
- OCR extraction from book covers
- Image text included in search and reasoning
- Enables text-based understanding from images

### 👤 Personalization
- Tracks basic user interactions
- Learns user preferences over time
- Improves recommendation relevance

### 🔐 Authentication
- JWT-based authentication
- OAuth 2.0 (Google login)
- Protected API routes

### 🏠 Local & Privacy-First
- No paid APIs
- No cloud-hosted LLMs
- Fully local embeddings and retrieval

---

## ⚡ How It Works
User Query
↓
FastAPI API
↓
Embedding Model
↓
Vector Database
↓
Relevant Books
↓
LLM Reasoning
↓
Final Response




---

## 🛠️ Tech Stack

### Backend
- FastAPI
- PostgreSQL (Supabase)
- JWT Authentication
- OAuth 2.0

### AI / ML
- SentenceTransformers (MPNet)
- ChromaDB
- Google Gemini
- Tesseract OCR

### Frontend
- React
- Tailwind CSS
- React Router

---

## 📁 Project Structure
BookBuddy/
├── backend/
├── frontend/
└── README.md


---

## 🚦 Getting Started

```bash
pip install -r requirements.txt
uvicorn backend.main:app --reload

Create .env:
GEMINI_API_KEY=your_key

👤 Author

Rajnish Kumar
GitHub: https://github.com/rajnishkumar1906
👤 Author

Rajnish Kumar
GitHub: https://github.com/rajnishkumar1906
