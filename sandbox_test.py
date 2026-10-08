import urllib.request
import json
import urllib.error

base_url = "http://localhost:8080/api"

def request(url, method="GET", data=None):
    req = urllib.request.Request(url, method=method)
    req.add_header('X-API-KEY', 'default-dev-key')
    if data:
        req.add_header('Content-Type', 'application/json')
        req.data = json.dumps(data).encode('utf-8')
    try:
        response = urllib.request.urlopen(req)
        return json.loads(response.read())
    except urllib.error.HTTPError as e:
        print(f"Error {e.code}: {e.read().decode('utf-8')}")
        return None

print("=== 1. Load Reference Banking Sandbox ===")
res = request(f"{base_url}/sandbox/load", method="POST")
print("Response:", json.dumps(res, indent=2))

print("\n=== 2. Verify endpoint exists ===")
endpoints = request(f"{base_url}/iso8583/endpoints")
if not endpoints:
    print("No endpoints found!")
    exit(1)
endpoint_id = endpoints[0]['id']
print(f"Found endpoint: {endpoints[0]['name']} (ID: {endpoint_id})")

print("\n=== 3. Verify scenario exists ===")
scenarios = request(f"{base_url}/scenarios")
if not scenarios:
    print("No scenarios found!")
    exit(1)
scenario = scenarios[0]
scenario_name = scenario['name']
scenario_id = scenario['id']
print(f"Found scenario: {scenario_name}")

def simulate(mti, fields):
    payload = {
        "mti": mti,
        "fields": fields
    }
    return request(f"{base_url}/iso8583/endpoints/{endpoint_id}/simulate", method="POST", data=payload)

def get_state():
    return request(f"{base_url}/scenarios/{scenario_id}/state/details")

print("\n=== 4. Send 0100 Authorization ===")
res_auth = simulate("0100", {"4": "1000"})
print("Response:", json.dumps(res_auth, indent=2))

print("\n=== 5. Verify AUTHORIZED state ===")
state_auth = get_state()
print("State:", state_auth['currentState'])
if state_auth['currentState'] != 'AUTHORIZED':
    print("FAILED! State is not AUTHORIZED.")

print("\n=== 6. Send 0220 Capture ===")
res_capture = simulate("0220", {"4": "1000"})
print("Response:", json.dumps(res_capture, indent=2))

print("\n=== 7. Verify CAPTURED state ===")
state_capture = get_state()
print("State:", state_capture['currentState'])
if state_capture['currentState'] != 'CAPTURED':
    print("FAILED! State is not CAPTURED.")

print("\n=== 8. Send 0420 Reversal ===")
res_reversal = simulate("0420", {"4": "1000"})
print("Response:", json.dumps(res_reversal, indent=2))

print("\n=== 9. Verify REVERSED state ===")
state_reversal = get_state()
print("State:", state_reversal['currentState'])
if state_reversal['currentState'] != 'REVERSED':
    print("FAILED! State is not REVERSED.")

print("\n=== DONE ===")
