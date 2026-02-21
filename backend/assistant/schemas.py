from pydantic import BaseModel
from typing import Dict, List


class AssistantRequest(BaseModel):
    question: str
    top_k: int = 5
    book_ids: List[str] | None = None  # optional: use these books first (e.g. for follow-ups)


class AssistantResponse(BaseModel):
    question: str
    answer: str
    citations: Dict[str, str]  # [1] -> book_id
    sources: List[dict]