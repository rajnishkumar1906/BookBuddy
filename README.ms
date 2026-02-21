# 📚 BookBuddy — Local AI Librarian (RAG-Based System)

BookBuddy is a **local, explainable AI-powered virtual librarian** built using **Retrieval-Augmented Generation (RAG)**.  
It can search, recommend, summarize, and explain books using **semantic understanding** rather than keyword matching — all while remaining **fully local and privacy-first**.

This project is designed as a **portfolio-grade system**, showcasing real-world practices across **backend engineering, machine learning, and web development**.

---

## 🎯 Project Overview

The goal of BookBuddy is to simulate the experience of interacting with a knowledgeable librarian who can:

- Understand natural language book queries  
- Recommend books based on meaning, not keywords  
- Explain *why* a book is recommended  
- Generate librarian-style summaries  
- Learn basic user preferences over time  
- Run entirely without paid APIs or cloud-hosted LLMs  

---

## ✅ Key Features

### 🔍 Semantic Book Search
- Vector-based search across titles, authors, genres, descriptions, and OCR text  
- Dense embeddings instead of traditional keyword matching  

### 🧠 Retrieval-Augmented Generation (RAG)
- Relevant books are retrieved from a vector database  
- A local LLM reasons over retrieved context  

### 📖 Explanations & Summaries
- Clear justification for recommendations  
- Librarian-style summaries (minimum 50 words)  
- Responses grounded in retrieved data  

### 🖼️ OCR-Based Text Understanding
- Extracts visible text from book covers using OCR  
- OCR text is embedded and indexed alongside metadata  
- Enables reasoning over image-derived text (no vision models)  

### 👤 User Preference Learning
- Tracks basic user interactions  
- Improves recommendations over time  

### 🔐 Authentication & Security
- JWT-based authentication  
- OAuth 2.0 (Google login)  
- Stateless and secure backend design  

### 🏠 Fully Local Execution
- No paid APIs  
- No cloud-hosted LLMs  
- Local embeddings, retrieval, and inference  

---

## 🧰 Technology Stack

### Backend
- **FastAPI** — high-performance REST APIs  
- **PostgreSQL (Supabase)** — structured data storage  
- **JWT Authentication** — secure access  
- **OAuth 2.0** — Google sign-in  

### AI / ML
- **ChromaDB** — vector database  
- **SentenceTransformers (MPNet)** — semantic embeddings  
- **Google Gemini** — LLM inference (API-key based)  
- **Tesseract OCR** — image text extraction  

### Frontend
- **React**  
- **Tailwind CSS**  
- **React Router**  
- **JWT-based auth flow**  
- **Protected routes & dashboards**  

---

## 🏗️ System Architecture

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

## 📂 Project Structure

```

backend/
├── main.py                  # FastAPI entry point
├── core/                    # Config, database, security
├── auth/                    # JWT & OAuth logic
├── users/                   # User profiles & preferences
├── books/                   # Book APIs
├── assistant/               # RAG + LLM logic
├── retrieval/               # Chroma semantic search
├── data_preprocessing/      # CSV cleaning & embeddings
├── chroma_store/            # Local persisted vectors

frontend/
├── src/
│   ├── pages/
│   ├── components/
│   ├── auth/
│   └── services/

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
Stores:
- Book metadata  
- Image URLs  
- User accounts  
- Interaction history  
- Preference signals  

### ChromaDB
Stores:
- Text embeddings  
- OCR-derived embeddings  
- Retrieval metadata  

---

## 🖼️ OCR & Image Handling

- Book covers referenced via image URLs  
- OCR performed using **Tesseract**  
- Extracted text embedded and indexed  
- Enables text-based reasoning from images  

---

## ❌ Intentionally Out of Scope

This version does **not** include:

- Direct image understanding by the LLM  
- Advanced computer vision pipelines  
- Agent graphs or multi-step planners  
- Cloud-based or paid LLM services  

This keeps the system:
- Explainable  
- Local  
- Debuggable  
- Maintainable  

---

## 🎯 Success Criteria

BookBuddy is successful when:

- Summaries feel librarian-like  
- Recommendations are semantically relevant  
- Explanations clearly justify results  
- User interaction improves recommendations  
- Everything runs locally and reproducibly  

---

## 🧪 Engineering Philosophy

- ML-first, API-second  
- Deterministic preprocessing  
- Clear separation of concerns  
- No hidden cloud dependencies  
- Debuggability over hype  

---

## 🛠️ Setup

```bash
pip install -r requirements.txt
uvicorn backend.main:app --reload
````

Create a `.env` file:

```env
GEMINI_API_KEY=your_key
```

```

---

If you want, I can:
- Make a **shorter “GitHub-landing” README**
- Add **badges + demo screenshots**
- Write a **resume-friendly project description**
- Or tailor it for **ML engineer vs backend roles**

Just say the word 👌
```
