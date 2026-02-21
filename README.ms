# 📚 BookBuddy — Local AI Librarian

**BookBuddy** is a local, explainable AI-powered virtual librarian built using Retrieval-Augmented Generation (RAG).  
It understands natural language queries, recommends books semantically, and explains *why* each recommendation makes sense — all while remaining **fully local and privacy-first**.

This project demonstrates real-world **ML engineering, backend architecture, and full-stack integration**, making it ideal for a portfolio showcase.

---

## 🚀 Features

### 🔍 Semantic Book Search
- Natural language book queries
- Vector-based semantic search (no keyword matching)
- Searches across titles, authors, genres, descriptions, and OCR text

### 🧠 Retrieval-Augmented Generation (RAG)
- Relevant books retrieved from a vector database
- Local LLM reasons over retrieved context
- Responses grounded in retrieved data

### 📖 Book Recommendations & Summaries
- Librarian-style explanations for recommendations
- Clear reasoning for *why* a book fits the query
- Book summaries (minimum 50 words)
- Neutral, informative librarian tone

### 🖼️ OCR-Based Text Understanding
- OCR extraction from book cover images using **Tesseract**
- OCR text embedded and indexed
- Enables text-based reasoning from images (no vision models)

### 👤 User Preference Learning
- Tracks basic user interactions
- Learns genre and topic preferences over time
- Improves recommendation relevance

### 🔐 Authentication & Security
- JWT-based authentication
- OAuth 2.0 (Google login)
- Stateless and secure backend
- Protected API routes

### 🏠 Fully Local Execution
- No paid APIs
- No cloud-hosted LLMs
- Local embeddings, retrieval, and inference
- Privacy-first by design

---

## ⚡ RAG System Architecture

```

User Query
↓
FastAPI API
↓
SentenceTransformers (Embeddings)
↓
Chroma Vector Store
↓
Relevant Book IDs
↓
PostgreSQL (Supabase)
↓
Book Metadata + OCR Text
↓
LLM (Gemini)
↓
Final Answer / Explanation

```

---

## 🛠️ Tech Stack

### Backend
- **FastAPI**
- **PostgreSQL (Supabase)**
- **JWT Authentication**
- **OAuth 2.0**

### AI / ML
- **ChromaDB**
- **SentenceTransformers (MPNet)**
- **Google Gemini**
- **Tesseract OCR**

### Frontend
- **React**
- **Tailwind CSS**
- **React Router**
- **JWT-based auth flow**

---

## 📁 Project Structure

```

BookBuddy/
├── backend/
│   ├── main.py
│   ├── core/
│   ├── auth/
│   ├── users/
│   ├── books/
│   ├── assistant/
│   ├── retrieval/
│   ├── data_preprocessing/
│   └── chroma_store/
│
├── frontend/
│   └── src/
│       ├── pages/
│       ├── components/
│       ├── auth/
│       └── services/
│
└── README.md

````

---

## 🔐 Authentication & Authorization
- JWT-based authentication
- Protected API routes
- OAuth 2.0 Google login
- Secure password hashing
- Stateless backend design

---

## 📊 Data Storage Strategy

### PostgreSQL (Supabase)
- Book metadata
- Image URLs
- User accounts
- Interaction history
- Preference signals

### ChromaDB
- Text embeddings
- OCR-derived embeddings
- Retrieval metadata

---

## 🖼️ OCR & Image Handling
- Book covers stored via image URLs
- OCR performed using **Tesseract**
- Extracted text embedded and indexed
- Enables text-based reasoning from images

---

## ❌ Out of Scope
- Direct image understanding by the LLM
- Advanced computer vision pipelines
- Agent graphs or multi-step planners
- Cloud-based or paid LLM services

---

## 🎯 Success Criteria
- Librarian-style summaries
- Semantically relevant recommendations
- Clear, explainable reasoning
- Local and reproducible execution

---

## 🧪 Engineering Philosophy
- ML-first, API-second
- Deterministic preprocessing
- Clear separation of concerns
- No hidden cloud dependencies
- Debuggability over hype

---

## 🚦 Getting Started

### Prerequisites
- Python 3.10+
- PostgreSQL
- Node.js

### Backend
```bash
pip install -r requirements.txt
uvicorn backend.main:app --reload
````

Create `.env`:

```env
GEMINI_API_KEY=your_key
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

---

## 🛣️ Roadmap

* [ ] Advanced preference modeling
* [ ] Conversation memory
* [ ] Hybrid retrieval (BM25 + vectors)
* [ ] Offline LLM inference
* [ ] Book similarity graph

