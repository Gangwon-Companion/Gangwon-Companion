FROM python:3.12-slim
WORKDIR /app
RUN pip install --no-cache-dir torch==2.6.0 --index-url https://download.pytorch.org/whl/cpu
RUN pip install --no-cache-dir sentence-transformers==3.4.1 fastapi==0.115.11 uvicorn==0.34.0
COPY server.py .
ENV HF_HOME=/models OMP_NUM_THREADS=2 TOKENIZERS_PARALLELISM=false
EXPOSE 8091
CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "8091"]
