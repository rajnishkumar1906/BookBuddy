# 📚 BookBuddy — Local AI Librarian

**BookBuddy** is a **local, explainable AI-powered virtual librarian** built using **Retrieval-Augmented Generation (RAG)**.  
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
