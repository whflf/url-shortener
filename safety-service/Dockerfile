FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt grpcio-tools
COPY . .

RUN python -m grpc_tools.protoc -I. --python_out=. --grpc_python_out=. safety.proto
CMD ["python", "safety_service.py"]