import grpc
from concurrent import futures
from transformers import BertTokenizerFast, BertForSequenceClassification, pipeline
import validators
import torch
import safety_pb2
import safety_pb2_grpc
import re

class SafetyService(safety_pb2_grpc.SafetyServiceServicer):
    def CheckUrl(self, request, context):
        url = request.url
        print(f"Checking URL: {url}")

        score = 0

        if url.count('.') > 3:
            score += 1

        if re.search(r'\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}', url):
            score += 2

        if '@' in url or url.count('-') > 4:
            score += 1

        bad_words = ['login', 'verify', 'update', 'banking', 'secure', 'ipfs']
        if any(word in url.lower() for word in bad_words):
            score += 1

        if re.search(r'\.(xyz|top|link|click|info)', url):
            score += 2

        if score >= 4 or not validators.url(url):
            print("UNSAFE")
            return safety_pb2.CheckUrlResponse(isSafe=False)

        device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        print(f"Using device: {device}")

        model_name = "CrabInHoney/urlbert-tiny-v4-phishing-classifier"
        tokenizer = BertTokenizerFast.from_pretrained(model_name)
        model = BertForSequenceClassification.from_pretrained(model_name)
        model.to(device)

        classifier = pipeline(
            "text-classification",
            model=model,
            tokenizer=tokenizer,
            device=0 if torch.cuda.is_available() else -1,
            top_k=1,
            truncation=True
        )

        is_safe = True

        result = classifier(url)[0][0]
        label = result['label']
        if label == "LABEL_1":      # LABEL_1 is label for fishing urls
            is_safe = False

        print("SAFE" if is_safe else "UNSAFE")
        return safety_pb2.CheckUrlResponse(isSafe=is_safe)

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    safety_pb2_grpc.add_SafetyServiceServicer_to_server(SafetyService(), server)
    server.add_insecure_port('[::]:9091')
    print("Python Safety Service started on port 9091...")
    server.start()
    server.wait_for_termination()

if __name__ == '__main__':
    serve()
