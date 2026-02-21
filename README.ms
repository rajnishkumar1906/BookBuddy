# 📚 BookBuddy

> An Explainable AI-Powered Virtual Librarian using RAG Architecture

BookBuddy is a full-stack AI-powered book discovery platform that uses **semantic search and Retrieval-Augmented Generation (RAG)** to provide intelligent, explainable book recommendations.

Unlike traditional keyword-based systems, BookBuddy understands user intent using embeddings and contextual reasoning.

---

## 🚀 Key Features

### 📖 Semantic Book Discovery
- Natural language search queries
- Embedding-based semantic retrieval
- No keyword dependency
- Context-aware results

### 🧠 Explainable AI Recommendations
- Retrieval-Augmented Generation (RAG)
- Clear reasoning for every suggestion
- Grounded responses based on retrieved documents

### 📝 Librarian-Style Summaries
- Minimum 50-word informative summaries
- Neutral, structured explanations
- Generated using LLM reasoning

### 🖼️ OCR-Based Image Understanding
- Extract text from book cover images
- Tesseract OCR integration
- Image text included in search pipeline

### 👤 Personalization Engine
- Tracks user interactions
- Learns genre and author preferences
- Improves recommendation relevance over time

### 🔐 Secure Authentication
- JWT-based authentication
- OAuth 2.0 (Google Login)
- Protected API routes
- Secure token handling

### 🏠 Privacy-Focused Architecture
- Local embedding generation
- Local vector database
- No paid APIs required
- Configurable LLM (local or Gemini)

---

## 🧠 System Architecture

```
User Query
     ↓
FastAPI Backend
     ↓
SentenceTransformers Embedding
     ↓
ChromaDB Vector Store
     ↓
Top-K Relevant Books Retrieved
     ↓
LLM (RAG Reasoning)
     ↓
Final Explainable Response
```

---

## 🛠️ Tech Stack

### 🔹 Backend
- FastAPI
- PostgreSQL (Supabase)
- JWT Authentication
- OAuth 2.0
- Uvicorn

### 🔹 AI / ML
- SentenceTransformers (all-mpnet-base-v2)
- ChromaDB (Vector Database)
- Google Gemini (Optional)
- Tesseract OCR
- RAG Pipeline Design

### 🔹 Frontend
- React
- Tailwind CSS
- React Router

---

## 📂 Project Structure

```
BookBuddy/
│
├── backend/
│   ├── app/
│   │   ├── routes/
│   │   ├── services/
│   │   ├── models/
│   │   ├── core/
│   │   └── main.py
│   │
│   ├── requirements.txt
│   └── .env
│
├── frontend/
│   ├── src/
│   ├── components/
│   ├── pages/
│   └── package.json
│
└── README.md
```

---

## ⚙️ Installation Guide

### 1️⃣ Clone Repository

```bash
git clone https://github.com/rajnishkumar1906/BookBuddy.git
cd BookBuddy
```

---

### 2️⃣ Backend Setup

```bash
cd backend
pip install -r requirements.txt
uvicorn app.main:app --reload
```

---

### 3️⃣ Create `.env` File

```env
GEMINI_API_KEY=your_key_here
DATABASE_URL=your_database_url_here
SECRET_KEY=your_secret_key_here
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
```

---

### 4️⃣ Frontend Setup

```bash
cd frontend
npm install
npm start
```

---

## 🔍 API Endpoints (Core)

| Method | Endpoint | Description |
|--------|----------|------------|
| POST | /auth/register | Register user |
| POST | /auth/login | Login user |
| POST | /search | Semantic book search |
| POST | /recommend | Personalized recommendations |
| POST | /upload-image | OCR extraction |
| GET | /profile | Get user profile |

---

## 📊 Machine Learning Pipeline

1. Book metadata is embedded using SentenceTransformers.
2. Embeddings are stored in ChromaDB.
3. User query is embedded.
4. Top-K similar books retrieved.
5. LLM generates explanation grounded in retrieved books.
6. Response returned with reasoning.

---

## 🎯 Why This Project Matters

This project demonstrates:

- Retrieval-Augmented Generation (RAG)
- Semantic search using vector embeddings
- Explainable AI system design
- Full-stack integration
- Authentication & security
- ML engineering best practices
- Real-world production architecture

It reflects how modern AI-powered recommendation systems are built.

---

## 📌 Future Improvements

- Fully local LLM integration (Mistral / Llama)
- Advanced user profiling
- Collaborative filtering hybrid system
- Book rating prediction model
- Admin dashboard
- Docker deployment

---

## 👤 Author

**Rajnish Kumar**  
GitHub: https://github.com/rajnishkumar1906  

---

## 📜 License

This project is licensed under the MIT License.

---

⭐ If you found this project useful, consider giving it a star!
