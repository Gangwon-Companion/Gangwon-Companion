"""CPU embedding service. No generated facts; pinned E5 with query/passage prefixes."""
import os
from threading import Lock
from typing import Literal

from fastapi import FastAPI
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer

MODEL = "intfloat/multilingual-e5-small"
REVISION = "614241f622f53c4eeff9890bdc4f31cfecc418b3"
model = SentenceTransformer(MODEL, revision=REVISION, device="cpu", trust_remote_code=False)
model.max_seq_length = 512
lock = Lock()
app = FastAPI()


class EmbedRequest(BaseModel):
    texts: list[str] = Field(min_length=1, max_length=32)
    kind: Literal["query", "passage"]


@app.get("/health")
def health():
    return {"model": MODEL, "revision": REVISION, "dimensions": 384, "normalization": "l2"}


@app.post("/embed")
def embed(request: EmbedRequest):
    from fastapi import HTTPException
    if any(not text.strip() or len(text) > 16000 for text in request.texts):
        raise HTTPException(422, "Texts must contain 1..16000 characters")
    with lock:
        vectors = model.encode([f"{request.kind}: {text}" for text in request.texts],
                               batch_size=16, normalize_embeddings=True).tolist()
    return {**health(), "vectors": vectors, "max_tokens": 512}
