#!/usr/bin/env python3
"""
Servidor HTTP simples para servir o APK para download
Uso: python3 scripts/servir-apk.py
"""

import http.server
import socketserver
import os
import sys
from pathlib import Path

# Porta padrão
PORT = 8000

# Encontrar o APK
apk_paths = [
    "app/build/outputs/apk/debug/app-debug.apk",
    "b/outputs/apk/debug/app-debug.apk"
]

apk_file = None
for path in apk_paths:
    if os.path.exists(path):
        apk_file = os.path.abspath(path)
        break

if not apk_file:
    print("❌ APK não encontrado!")
    print("💡 Execute: ./gradlew :app:assembleDebug")
    sys.exit(1)

apk_name = os.path.basename(apk_file)
apk_size = os.path.getsize(apk_file)

print("=" * 60)
print("📱 SERVIDOR DE DOWNLOAD DE APK")
print("=" * 60)
print(f"📦 APK: {apk_name}")
print(f"📊 Tamanho: {apk_size / (1024*1024):.2f} MB")
print(f"📁 Caminho: {apk_file}")
print("=" * 60)
print()

class APKHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/' or self.path == '/apk' or self.path == f'/{apk_name}':
            self.send_response(200)
            self.send_header('Content-Type', 'application/vnd.android.package-archive')
            self.send_header('Content-Disposition', f'attachment; filename="{apk_name}"')
            self.send_header('Content-Length', str(apk_size))
            self.end_headers()
            
            with open(apk_file, 'rb') as f:
                self.wfile.write(f.read())
        else:
            # Página HTML simples
            html = f"""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Download APK - GestaoBilhares</title>
    <style>
        body {{
            font-family: Arial, sans-serif;
            max-width: 600px;
            margin: 50px auto;
            padding: 20px;
            background: #f5f5f5;
        }}
        .container {{
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }}
        h1 {{ color: #333; }}
        .info {{
            background: #e3f2fd;
            padding: 15px;
            border-radius: 5px;
            margin: 20px 0;
        }}
        .download-btn {{
            display: inline-block;
            background: #4CAF50;
            color: white;
            padding: 15px 30px;
            text-decoration: none;
            border-radius: 5px;
            font-size: 18px;
            margin: 20px 0;
        }}
        .download-btn:hover {{
            background: #45a049;
        }}
    </style>
</head>
<body>
    <div class="container">
        <h1>📱 Download APK</h1>
        <div class="info">
            <p><strong>Arquivo:</strong> {apk_name}</p>
            <p><strong>Tamanho:</strong> {apk_size / (1024*1024):.2f} MB</p>
        </div>
        <a href="/{apk_name}" class="download-btn">⬇️ Baixar APK</a>
        <p style="color: #666; margin-top: 20px;">
            💡 Após baixar, ative "Fontes desconhecidas" nas configurações do Android para instalar.
        </p>
    </div>
</body>
</html>
            """
            self.send_response(200)
            self.send_header('Content-Type', 'text/html; charset=utf-8')
            self.end_headers()
            self.wfile.write(html.encode('utf-8'))

try:
    with socketserver.TCPServer(("", PORT), APKHandler) as httpd:
        print(f"🚀 Servidor iniciado em:")
        print(f"   http://localhost:{PORT}")
        print(f"   http://0.0.0.0:{PORT}")
        print()
        print(f"📥 Link direto para download:")
        print(f"   http://localhost:{PORT}/{apk_name}")
        print()
        print("⚠️  Para acessar de outro computador, use o IP da VM")
        print("   (Pressione Ctrl+C para parar o servidor)")
        print()
        httpd.serve_forever()
except KeyboardInterrupt:
    print("\n\n✅ Servidor encerrado")
except OSError as e:
    if "Address already in use" in str(e):
        print(f"❌ Porta {PORT} já está em uso!")
        print(f"💡 Tente outra porta: python3 scripts/servir-apk.py {PORT + 1}")
    else:
        print(f"❌ Erro: {e}")
