import { test, expect } from '@playwright/test';

test.use({
  baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost',
  channel: 'chrome',
});

const STANDALONE_LOGIN_HTML = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Mobile Banking Login</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: rgb(15, 23, 42);
      color: #f8fafc;
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
    }
    .banking-card {
      width: 100%;
      max-width: 380px;
      background: rgb(30, 41, 59);
      border: 1px solid #334155;
      border-radius: 20px;
      padding: 28px 24px;
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
    }
    .bank-badge {
      display: inline-block;
      background: rgba(16, 185, 129, 0.15);
      color: #34d399;
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      padding: 4px 10px;
      border-radius: 999px;
      margin-bottom: 12px;
    }
    h1 {
      font-size: 22px;
      font-weight: 700;
      margin-bottom: 6px;
    }
    .subtitle {
      font-size: 13px;
      color: #94a3b8;
      margin-bottom: 22px;
    }
    .field {
      margin-bottom: 16px;
    }
    label {
      display: block;
      font-size: 12px;
      font-weight: 600;
      color: #cbd5e1;
      margin-bottom: 6px;
    }
    input {
      width: 100%;
      padding: 11px 14px;
      border-radius: 10px;
      border: 1px solid #475569;
      background: #0f172a;
      color: #f8fafc;
      font-size: 14px;
      outline: none;
    }
    #login-btn {
      width: 100%;
      padding: 12px;
      border: none;
      border-radius: 10px;
      background: #10b981;
      color: #06281e;
      font-size: 15px;
      font-weight: 700;
      cursor: pointer;
      margin-top: 6px;
    }
    #login-status {
      margin-top: 16px;
      font-size: 13px;
      text-align: center;
      color: #34d399;
      min-height: 20px;
    }
  </style>
</head>
<body>
  <div class="banking-card" id="mobile-banking-login-card">
    <span class="bank-badge">Mobile Banking</span>
    <h1>Secure Sign In</h1>
    <p class="subtitle">Authenticate to continue to your mobile banking session</p>
    <form id="mobile-login-form">
      <div class="field">
        <label for="mobile-number">Mobile Number / User ID</label>
        <input id="mobile-number" type="text" value="9800000000" required />
      </div>
      <div class="field">
        <label for="mpin">MPIN / Password</label>
        <input id="mpin" type="password" value="1234" required />
      </div>
      <button id="login-btn" type="submit">Login</button>
    </form>
    <div id="login-status" data-return-url="__RETURN_URL__"></div>
  </div>
  <script>
    (function() {
      var form = document.getElementById('mobile-login-form');
      var statusEl = document.getElementById('login-status');
      var returnUrl = statusEl.getAttribute('data-return-url');
      form.addEventListener('submit', function(e) {
        e.preventDefault();
        statusEl.textContent = 'Processing login...';
        var triggerUrl = window.location.pathname + '?action=login&returnUrl=' + encodeURIComponent(returnUrl);
        fetch(triggerUrl, { method: 'GET' })
          .then(function(res) { return res.text(); })
          .then(function() {
            statusEl.textContent = 'Login completed - return URL triggered';
          })
          .catch(function() {
            statusEl.textContent = 'Login trigger failed';
          });
      });
    })();
  </script>
</body>
</html>`;

const POLYGLOT_POST_SCRIPT = `var returnUrl = (request.queryParams && request.queryParams.returnUrl) ||
                (request.query && request.query.returnUrl) ||
                (request.body && typeof request.body === 'object' ? request.body.returnUrl : null);

if (!returnUrl && request.body && typeof request.body === 'string' && request.body.trim().length > 0) {
  try {
    var parsed = JSON.parse(request.body);
    if (parsed && parsed.returnUrl) {
      returnUrl = parsed.returnUrl;
    }
  } catch (e) {
    var match = request.body.match(/(?:^|&)returnUrl=([^&]+)/);
    if (match) {
      returnUrl = decodeURIComponent(match[1]);
    }
  }
}

var action = (request.queryParams && request.queryParams.action) ||
             (request.query && request.query.action) ||
             (request.body && typeof request.body === 'object' ? request.body.action : null);

response.headers = { 'Content-Type': 'text/html; charset=utf-8' };

if (!returnUrl || String(returnUrl).trim() === '') {
  response.body = 'no return url were provided';
} else if (action === 'login') {
  var cleanReturnUrl = String(returnUrl).trim();
  var internalReturnUrl = cleanReturnUrl.replace(/^http:\\/\\/localhost\\/mock\\//, 'http://localhost:8080/mock/');
  webhook = {
    url: internalReturnUrl,
    method: 'GET'
  };
  response.body = 'Login completed - return URL triggered';
} else {
  response.body = response.body.replace(/__RETURN_URL__/g, String(returnUrl).trim());
}`;

test.describe('Release v0.6.1 - Interactive Simulation Experience E2E', () => {
  let authToken = '';

  test.beforeAll(async ({ request, baseURL }) => {
    // 1. Authenticate with admin credentials
    const loginRes = await request.post(`${baseURL}/api/auth/login`, {
      data: { username: 'admin', password: 'admin' },
    });
    expect(loginRes.ok()).toBeTruthy();
    const loginData = await loginRes.json();
    authToken = loginData.token;
    const authHeaders = { Authorization: `Bearer ${authToken}` };

    // 2. Clean up existing test routes
    const routesRes = await request.get(`${baseURL}/api/routes`, { headers: authHeaders });
    if (routesRes.ok()) {
      const routes = await routesRes.json();
      for (const r of routes) {
        if (
          [
            '/interactive-mobile-login',
            '/return-redirector',
            '/return-destination',
            '/api/json-sample',
            '/api/xml-sample',
          ].includes(r.path)
        ) {
          await request.delete(`${baseURL}/api/routes/${r.id}`, { headers: authHeaders });
        }
      }
    }

    async function createAndActivateRoute(data: any) {
      const res = await request.post(`${baseURL}/api/routes`, {
        headers: authHeaders,
        data,
      });
      const created = await res.json();
      await request.post(`${baseURL}/api/routes/${created.id}/activate`, {
        headers: authHeaders,
      });
      return created;
    }

    // 3. Create Destination Stub (/return-destination)
    await createAndActivateRoute({
      path: '/return-destination',
      method: 'GET',
      responseStatus: 200,
      responseHeaders: { 'Content-Type': 'application/json' },
      responseTemplate: '{"status":"redirect_followed_ok","finalDestination":true}',
    });

    // 4. Create Redirector Stub (/return-redirector -> 302 Location: http://localhost:8080/mock/return-destination)
    await createAndActivateRoute({
      path: '/return-redirector',
      method: 'GET',
      responseStatus: 302,
      responseHeaders: {
        Location: 'http://localhost:8080/mock/return-destination',
        'Content-Type': 'application/json',
      },
      responseTemplate: '{"status":"redirecting"}',
    });

    // 5. Create Mobile Banking Login Stub (/interactive-mobile-login)
    await createAndActivateRoute({
      path: '/interactive-mobile-login',
      method: 'GET',
      responseStatus: 200,
      responseHeaders: { 'Content-Type': 'text/html; charset=utf-8' },
      responseTemplate: STANDALONE_LOGIN_HTML,
      postScript: POLYGLOT_POST_SCRIPT,
    });

    // 6. Create JSON Stub for Backward Compatibility check
    await createAndActivateRoute({
      path: '/api/json-sample',
      method: 'GET',
      responseStatus: 200,
      responseHeaders: { 'Content-Type': 'application/json' },
      responseTemplate: '{"message":"json backward compatibility preserved"}',
    });

    // 7. Create XML Stub for Backward Compatibility check
    await createAndActivateRoute({
      path: '/api/xml-sample',
      method: 'GET',
      responseStatus: 200,
      responseHeaders: { 'Content-Type': 'application/xml' },
      responseTemplate: '<response><status>ok</status></response>',
    });
  });

  test('Full Interactive Simulation Journey: HTML Preview, Sandboxed Execution, Redirect Chain, Console Telemetry & Backward Compatibility', async ({
    page,
    baseURL,
  }) => {
    // Navigate to UI and sign in
    await page.goto(`${baseURL}/login`);
    await page.locator('#login-username').fill('admin');
    await page.locator('#login-password').fill('admin');
    await page.locator('#login-submit').click();

    // Verify workspace loaded
    await expect(page.locator('button[title="Add new stub"]')).toBeVisible({ timeout: 15000 });

    // -------------------------------------------------------------------------
    // TEST 1: Execute Mobile Banking Login Stub & Verify HTML Response Preview
    // -------------------------------------------------------------------------
    const routeItem = page.locator('button:has-text("/interactive-mobile-login")').first();
    await expect(routeItem).toBeVisible({ timeout: 10000 });
    await routeItem.click();

    // Fill target URL with returnUrl query parameter
    const urlInput = page.locator('input[placeholder="{{BASE_URL}}/api/example"]');
    await expect(urlInput).toBeVisible();
    const targetUrl = `{{BASE_URL}}/interactive-mobile-login?returnUrl=http://localhost/mock/return-redirector`;
    await urlInput.fill(targetUrl);

    // Track direct mock endpoint hits to prove ZERO DOUBLE EXECUTION
    let directMockHits = 0;
    page.on('request', (req) => {
      if (req.url().includes('/mock/interactive-mobile-login')) {
        directMockHits++;
      }
    });

    // Click Send in Test Client
    const sendButton = page.locator('button[title="Execute test request (⌘+Enter)"]');
    await sendButton.click();

    // Verify ResponsePanel renders and automatically activates the Preview tab
    const previewTab = page.locator('[data-testid="response-tab-preview"]');
    await expect(previewTab).toBeVisible({ timeout: 10000 });
    await expect(previewTab).toHaveClass(/border-primary/);

    // -------------------------------------------------------------------------
    // SECURITY VALIDATION: Inspect Sandbox Invariants on Preview Iframe
    // -------------------------------------------------------------------------
    const previewIframe = page.locator('iframe[data-testid="html-preview-iframe"]');
    await expect(previewIframe).toBeVisible({ timeout: 10000 });

    const sandboxAttr = await previewIframe.getAttribute('sandbox');
    expect(sandboxAttr).toBe('allow-scripts allow-forms allow-popups');
    expect(sandboxAttr).not.toContain('allow-same-origin');
    expect(sandboxAttr).not.toContain('allow-top-navigation');

    const referrerPolicy = await previewIframe.getAttribute('referrerpolicy');
    expect(referrerPolicy).toBe('no-referrer');

    // Prove ZERO DOUBLE EXECUTION: The preview iframe uses srcdoc, NOT src="/mock/..."
    const iframeSrc = await previewIframe.getAttribute('src');
    expect(iframeSrc).toBeNull();
    const iframeSrcDoc = await previewIframe.getAttribute('srcdoc');
    expect(iframeSrcDoc).toContain('Secure Sign In');
    // Direct requests to /mock/ remain 0 because execution occurred via backend proxy
    expect(directMockHits).toBe(0);

    // -------------------------------------------------------------------------
    // INTERACTION INSIDE PREVIEW IFRAME: Fill credentials & trigger login
    // -------------------------------------------------------------------------
    const frame = page.frameLocator('iframe[data-testid="html-preview-iframe"]');
    await expect(frame.locator('#mobile-banking-login-card')).toBeVisible();
    await expect(frame.locator('h1')).toHaveText('Secure Sign In');

    await frame.locator('#mobile-number').fill('9841234567');
    await frame.locator('#mpin').fill('5678');
    await page.waitForTimeout(500);
    await frame.locator('#login-btn').click();

    // Verify status update inside the iframe
    await expect(frame.locator('#login-status')).toHaveText('Login completed - return URL triggered', {
      timeout: 10000,
    });

    // -------------------------------------------------------------------------
    // TEST 2: Redirect Chain Visualization in Test Client
    // -------------------------------------------------------------------------
    // Select /return-redirector stub to verify redirect chain banner
    const redirectorRouteItem = page.locator('button:has-text("/return-redirector")').first();
    await redirectorRouteItem.click();

    await sendButton.click();

    // Verify RedirectChainBanner is rendered
    const redirectBanner = page.locator('[data-testid="redirect-chain-banner"]');
    await expect(redirectBanner).toBeVisible({ timeout: 10000 });
    await expect(page.locator('[data-testid="redirect-chain-hop-count"]')).toHaveText('2 Hops');
    await expect(page.locator('[data-testid="redirect-hop-status-1"]')).toHaveText('302');
    await expect(page.locator('[data-testid="redirect-hop-location-1"]')).toContainText('/mock/return-destination');
    await expect(page.locator('[data-testid="redirect-hop-status-2"]')).toHaveText('200');

    // -------------------------------------------------------------------------
    // TEST 3: Console Explorer - Expandable Telemetry, Query Params & IDs
    // -------------------------------------------------------------------------
    const consolePanel = page.locator('[data-testid="bottom-bar-panel"]');
    await expect(consolePanel).toBeVisible();

    // Search filtering in BottomBar
    const searchInput = page.locator('[data-testid="traffic-search-input"]');
    await searchInput.fill('interactive-mobile-login');
    const filteredRows = page.locator('[data-testid^="traffic-event-row-"]');
    await expect(filteredRows.first()).toBeVisible({ timeout: 10000 });

    // Verify query string token in collapsed row
    const queryToken = page.locator('[data-testid="traffic-row-query"]').first();
    await expect(queryToken).toContainText('?action=login');

    // Verify sub-millisecond duration format (\d+(\.\d+)?ms)
    const durationDisplay = page.locator('[data-testid="traffic-row-duration"]').first();
    const durationText = await durationDisplay.textContent();
    expect(durationText).toMatch(/\d+(\.\d+)?\s*ms/);

    // Expand the traffic event row
    const rowHeader = page.locator('[data-testid^="traffic-row-header-"]').first();
    await rowHeader.click();

    // Verify Full Request ID and Route ID visibility with copy controls
    const requestIdEl = page.locator('[data-testid="traffic-detail-req-id"]');
    await expect(requestIdEl).toBeVisible();
    const fullRequestId = await requestIdEl.textContent();
    expect(fullRequestId?.length).toBeGreaterThan(10);
    await expect(page.locator('[data-testid="traffic-copy-req-id"]')).toBeVisible();

    const routeIdEl = page.locator('[data-testid="traffic-detail-route-id"]');
    await expect(routeIdEl).toBeVisible();
    await expect(page.locator('[data-testid="traffic-copy-route-id"]')).toBeVisible();

    // Verify Decoded Query Parameters table in Details tab
    const queryParamsTab = page.locator('[data-testid="traffic-tab-params"]');
    await queryParamsTab.click();
    const queryTable = page.locator('[data-testid="traffic-query-params-table"]');
    await expect(queryTable).toBeVisible();
    await expect(queryTable).toContainText('action');
    await expect(queryTable).toContainText('login');
    await expect(queryTable).toContainText('returnUrl');

    // Verify Protocol Filter Pills
    await searchInput.clear();
    await page.locator('[data-testid="traffic-filter-webhook"]').click();
    // After selecting WEBHOOK, verify only webhook protocol events are shown
    const webhookBadge = page.locator('[data-testid="traffic-row-protocol"]:has-text("WEBHOOK")').first();
    await expect(webhookBadge).toBeVisible();
    // Reset filter to all
    await page.locator('[data-testid="traffic-filter-all"]').click();

    // -------------------------------------------------------------------------
    // TEST 4: RouteDrawer Integration - Source Default & Static Preview Toggle
    // -------------------------------------------------------------------------
    // Re-select /interactive-mobile-login
    await page.locator('button:has-text("/interactive-mobile-login")').first().click();

    // Open Route Settings Drawer
    const routeSettingsBtn = page.locator('button[title="Route Settings"]').first();
    await routeSettingsBtn.click();

    // MANDATORY REQUIREMENT: Defaults to Source tab (NOT Preview)
    const sourceTab = page.locator('[data-testid="route-template-tab-source"]');
    await expect(sourceTab).toBeVisible({ timeout: 10000 });
    await expect(sourceTab).toHaveAttribute('aria-selected', 'true');
    await expect(page.locator('pre:has-text("Mobile Banking Login")')).toBeVisible();

    // Switch to Preview tab in RouteDrawer
    const drawerPreviewTab = page.locator('[data-testid="route-template-tab-preview"]');
    await drawerPreviewTab.click();

    // MANDATORY SECURITY REQUIREMENT: RouteDrawer preview enforces sandbox="" (Static)
    const drawerIframe = page.locator('[data-testid="route-template-preview"] iframe[data-testid="html-preview-iframe"]');
    await expect(drawerIframe).toBeVisible();
    const staticSandboxAttr = await drawerIframe.getAttribute('sandbox');
    expect(staticSandboxAttr).toBe('');
    expect(staticSandboxAttr).not.toContain('allow-scripts');

    // Close RouteDrawer by clicking the backdrop or close button
    const closeDrawerBtn = page.locator('div.fixed:has-text("Route Details") button').first();
    if (await closeDrawerBtn.isVisible()) {
      await closeDrawerBtn.click();
    }

    // -------------------------------------------------------------------------
    // TEST 5: Backward Compatibility - JSON & XML Responses
    // -------------------------------------------------------------------------
    // Execute JSON route
    const jsonRouteItem = page.locator('button:has-text("/api/json-sample")').first();
    await jsonRouteItem.click();
    await sendButton.click();

    // Verify Preview tab is NOT present for JSON responses
    await expect(page.locator('[data-testid="response-tab-preview"]')).toHaveCount(0);
    const bodyTab = page.locator('[data-testid="response-tab-body"]');
    await expect(bodyTab).toBeVisible();
    await expect(bodyTab).toHaveClass(/border-primary/);

    // Execute XML route
    const xmlRouteItem = page.locator('button:has-text("/api/xml-sample")').first();
    await xmlRouteItem.click();
    await sendButton.click();

    // Verify Preview tab is NOT present for XML responses
    await expect(page.locator('[data-testid="response-tab-preview"]')).toHaveCount(0);
    await expect(bodyTab).toHaveClass(/border-primary/);
  });
});
