📚 BookBuddy — Local AI Librarian (RAG-Based System)

BookBuddy is a local, explainable AI-powered virtual librarian built using Retrieval-Augmented Generation (RAG).
It can search, recommend, summarize, and explain books using semantic understanding rather than keyword matching — all while remaining fully local and privacy-first.

This project is designed as a portfolio-grade system, showcasing real-world practices across backend engineering, machine learning, and web development.

🎯 Project Overview

The goal of BookBuddy is to simulate the experience of interacting with a knowledgeable librarian who can:

Understand natural language book queries

Recommend books based on meaning, not keywords

Explain why a book is recommended

Generate librarian-style summaries

Learn basic user preferences over time

Run entirely without paid APIs or cloud-hosted LLMs

✅ Key Features
🔍 Semantic Book Search

Vector-based search across titles, authors, genres, descriptions, and OCR text

Uses dense embeddings instead of traditional keyword matching

🧠 Retrieval-Augmented Generation (RAG)

Relevant books are retrieved from a vector database

A local LLM reasons over retrieved context to generate responses

📖 Explanations & Summaries

Clear justifications for recommendations

Librarian-style summaries (minimum 50 words)

Transparent reasoning grounded in retrieved data

🖼️ OCR-Based Text Understanding

Extracts visible text from book covers using OCR

OCR text is embedded and indexed alongside structured metadata

Enables reasoning over image-derived text without raw vision models

👤 User Preference Learning

Tracks basic user interactions

Gradually improves recommendations over time

🔐 Authentication & Security

JWT-based authentication

OAuth 2.0 (Google sign-in)

Stateless, secure backend architecture

🏠 Fully Local Execution

No paid APIs

No cloud-hosted LLM dependency

Embeddings, inference, and retrieval run locally

🧰 Technology Stack
Backend

FastAPI — high-performance REST API framework

PostgreSQL (Supabase) — structured data storage

JWT Authentication — secure API access

OAuth 2.0 — Google login support

AI / ML

ChromaDB — vector database for semantic retrieval

SentenceTransformers (MPNet) — dense text embeddings

Google Gemini — LLM inference (local orchestration via API key)

Tesseract OCR — text extraction from book cover images

Frontend

React

Tailwind CSS

React Router

JWT-based authentication flow

Protected routes and dashboards

🏗️ System Architecture
User Query
↓
FastAPI API
↓
Embedding Model (SentenceTransformers)
↓
Chroma Vector Store
↓
Relevant Book IDs
↓
Supabase (PostgreSQL)
↓
Book Metadata + OCR Text
↓
LLM (Gemini)
↓
Final Answer / Explanation
📂 Project Structure
backend/
│
├── main.py                  # FastAPI entry point
├── core/                    # Configuration, database, security
│
├── auth/                    # JWT & OAuth logic
├── users/                   # User profiles & preferences
├── books/                   # Book search & detail APIs
├── assistant/               # RAG + LLM orchestration
│
├── retrieval/               # Chroma-based semantic search
├── data_preprocessing/      # CSV cleaning & embedding pipeline
├── chroma_store/            # Persisted local embeddings
│
frontend/
├── src/
│   ├── pages/
│   ├── components/
│   ├── auth/
│   └── services/
🔐 Authentication & Authorization

JWT-based authentication

Access tokens

Protected API routes

OAuth 2.0 integration

Google login support

Secure password hashing

Stateless authentication design

📊 Data Storage Strategy
PostgreSQL (Supabase)

Stores:

Book metadata

Image URLs

User accounts

Interaction history

Preference signals

ChromaDB

Stores:

Text embeddings

OCR-derived embeddings

Metadata for semantic retrieval

🖼️ OCR & Image Processing

Book covers referenced via image URLs

OCR performed using Tesseract

Extracted text embedded and indexed

Enables text-based reasoning from images (no direct vision models)

❌ Intentionally Out of Scope

This version deliberately excludes:

Direct image understanding by the LLM

Advanced computer vision pipelines

Agent graphs or multi-step planners

Cloud-based or paid LLM services

This ensures the system remains:

Explainable

Local

Debuggable

Maintainable

🎯 Success Criteria

BookBuddy is considered successful when:

Summaries feel librarian-like

Recommendations are semantically relevant

Explanations clearly justify recommendations

The system improves via user interaction data

Everything runs locally and reproducibly

🧪 Engineering Philosophy

ML-first, API-second

Deterministic preprocessing pipelines

Clear separation of concerns

No hidden cloud dependencies

Debuggability over hype

🛠️ High-Level Setup
# Install dependencies
pip install -r requirements.txt

# Run backend
uvicorn backend.main:app --reload

# Set Gemini API key in .env
# GEMINI_API_KEY=your_key
