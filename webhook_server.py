#!/usr/bin/env python3
"""
Servidor webhook de ejemplo para recibir notificaciones del plugin
Guarda eventos en un archivo de log y los muestra en consola
"""

from flask import Flask, request
import json
import os
from datetime import datetime

app = Flask(__name__)

# Directorio de logs
LOG_DIR = '/var/log/webhook'
os.makedirs(LOG_DIR, exist_ok=True)
LOG_FILE = os.path.join(LOG_DIR, 'webhook_events.log')

def log_event(event_data):
    """Guarda el evento en archivo de log"""
    with open(LOG_FILE, 'a') as f:
        f.write(json.dumps({
            'timestamp': datetime.now().isoformat(),
            'event': event_data
        }) + '\n')

def print_event(data):
    """Imprime el evento en consola de forma legible"""
    print("\n" + "═" * 80)
    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] WEBHOOK EVENT RECEIVED")
    print("═" * 80)
    print(f"Type: {data.get('event_type', 'N/A')}")
    print(f"Enode: {data.get('enode', 'N/A')}")
    print(f"Items Count: {data.get('items_count', 0)}")
    print(f"Items: {', '.join(data.get('items', []))}")
    print(f"Timestamp: {data.get('timestamp', 'N/A')}")
    print("═" * 80 + "\n")

@app.route('/webhook', methods=['POST'])
def webhook():
    """Endpoint principal para recibir webhooks"""
    try:
        data = request.json
        
        # Registrar evento
        log_event(data)
        
        # Mostrar en consola
        print_event(data)
        
        # Responder
        return {
            'status': 'success',
            'message': 'Event received',
            'timestamp': datetime.now().isoformat()
        }, 200
        
    except Exception as e:
        print(f"[ERROR] {str(e)}")
        return {
            'status': 'error',
            'message': str(e)
        }, 500

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return {
        'status': 'healthy',
        'service': 'besu-permission-webhook',
        'timestamp': datetime.now().isoformat()
    }, 200

@app.route('/events', methods=['GET'])
def get_events():
    """Obtener eventos guardados"""
    try:
        if not os.path.exists(LOG_FILE):
            return {'events': [], 'total': 0}, 200
        
        events = []
        with open(LOG_FILE, 'r') as f:
            for line in f:
                events.append(json.loads(line))
        
        return {
            'total': len(events),
            'events': events
        }, 200
    except Exception as e:
        return {'error': str(e)}, 500

@app.route('/stats', methods=['GET'])
def get_stats():
    """Obtener estadísticas de eventos"""
    try:
        if not os.path.exists(LOG_FILE):
            return {
                'total_events': 0,
                'by_type': {},
                'file_size': 0
            }, 200
        
        by_type = {}
        with open(LOG_FILE, 'r') as f:
            for line in f:
                data = json.loads(line)
                event_type = data.get('event', {}).get('event_type', 'unknown')
                by_type[event_type] = by_type.get(event_type, 0) + 1
        
        file_size = os.path.getsize(LOG_FILE)
        
        return {
            'total_events': len(by_type),
            'by_type': by_type,
            'file_size_bytes': file_size,
            'log_file': LOG_FILE
        }, 200
    except Exception as e:
        return {'error': str(e)}, 500

@app.route('/clear', methods=['POST'])
def clear_events():
    """Limpiar eventos guardados (solo para desarrollo)"""
    try:
        if os.path.exists(LOG_FILE):
            os.remove(LOG_FILE)
            return {'status': 'success', 'message': 'Events cleared'}, 200
        else:
            return {'status': 'success', 'message': 'No events to clear'}, 200
    except Exception as e:
        return {'error': str(e)}, 500

@app.route('/', methods=['GET'])
def index():
    """Información del servidor"""
    return {
        'name': 'Besu Permission Webhook Receiver',
        'version': '1.0.0',
        'endpoints': {
            'POST /webhook': 'Recibir eventos del plugin',
            'GET /events': 'Obtener eventos guardados',
            'GET /stats': 'Obtener estadísticas',
            'GET /health': 'Health check',
            'POST /clear': 'Limpiar eventos (dev only)'
        },
        'log_file': LOG_FILE
    }, 200

if __name__ == '__main__':
    print("\n" + "═" * 80)
    print("Besu Permission Webhook Receiver")
    print("═" * 80)
    print(f"Listening on: http://0.0.0.0:8000")
    print(f"Log file: {LOG_FILE}")
    print(f"Endpoints:")
    print(f"  POST /webhook         - Recibir eventos")
    print(f"  GET  /events          - Ver eventos")
    print(f"  GET  /stats           - Ver estadísticas")
    print(f"  GET  /health          - Health check")
    print(f"  POST /clear           - Limpiar eventos")
    print("═" * 80 + "\n")
    
    app.run(host='0.0.0.0', port=8000, debug=True)
