from typing import List, Dict
from llm.ollama_client import ask_ollama

def librarian_answer(question: str, books: List[Dict]) -> str:
    context = "\n".join(
        f"{i+1}. {b['title']} by {b['author']} ({b['genres']})"
        for i, b in enumerate(books)
    )

    prompt = f"""
You are a helpful librarian.
Use ONLY the books below.

BOOKS:
{context}

QUESTION:
{question}

ANSWER:
"""
    return ask_ollama(prompt)