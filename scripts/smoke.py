#!/usr/bin/env python3
"""Smoke test against the two local running services. Creates an isolated test event and simulated purchases."""
import datetime, json, os, urllib.request, urllib.error, uuid
B = os.getenv('BILHETERIA_URL', 'http://localhost:8080')
P = os.getenv('PAGAMENTOS_URL', 'http://localhost:8081')
def request(base, path, data=None, expected=200, headers=None):
    req=urllib.request.Request(base+path, data=json.dumps(data).encode() if data is not None else None,headers={'Content-Type':'application/json', **(headers or {})})
    try:
        with urllib.request.urlopen(req,timeout=8) as r: code,body=r.status,json.load(r)
    except urllib.error.HTTPError as e: code,body=e.code,json.load(e)
    assert code==expected,(path,code,body)
    return body
future=(datetime.date.today()+datetime.timedelta(days=30)).isoformat()
e=request(B,'/eventos',{'nome':'Teste integrado '+str(uuid.uuid4())[:8], 'data':future,'preco':150,'vagas':3})
id=e['id']
bad=request(P,'/cobrancas',{'valor':299,'cartao_final':'4242','evento_id':id,'quantidade':2},422)
assert bad['codigo']=='valor_divergente'
missing=request(P,'/cobrancas',{'valor':300,'cartao_final':'4242','evento_id':2147483647,'quantidade':2},422)
assert missing['codigo']=='regra_negocio'
refused=request(B,f'/eventos/{id}/comprar',{'cpf':'12345678901','quantidade':2,'cartao_final':'0000'},422)
assert refused['codigo']=='regra_negocio'
assert request(B,f'/eventos/{id}')['vagas']==3
ticket=request(B,f'/eventos/{id}/comprar',{'cpf':'12345678901','quantidade':2,'cartao_final':'4242'})
assert ticket['status']=='PAGO'
assert request(P,f"/cobrancas/{ticket['cobranca_id']}")['status']=='APROVADA'
assert request(B,f'/eventos/{id}')['vagas']==1
request(B,f'/eventos/{id}/comprar',{'cpf':'12345678901','quantidade':2,'cartao_final':'4242'},422)
assert request(B,f'/eventos/{id}')['vagas']==1
print('OK: preço divergente, evento inexistente, recusa sem baixa, aprovação, cobrança consultável e estoque insuficiente.')
print(f'Evento de teste {id} criado; compra simulada preservada para inspeção.')
