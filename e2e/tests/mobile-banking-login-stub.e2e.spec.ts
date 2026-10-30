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
      background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
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
    input:focus {
      border-color: #10b981;
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
      transition: background 0.15s ease;
    }
    #login-btn:hover {
      background: #059669;
      color: #ffffff;
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

test.describe('Dynamic Mock UI - Custom HTML Mobile Banking Login Stub E2E', () => {
  test('configures stub in UI, renders HTML without calling returnUrl, calls redirecting returnUrl only on Login click, and handles missing returnUrl', async ({ browser, request, baseURL }) => {
    // 0. Clean up any pre-existing test stubs with the same paths via API before starting the UI flow
    const loginApiRes = await request.post(`${baseURL}/api/auth/login`, {
      data: { username: 'admin', password: 'admin' },
    });
    expect(loginApiRes.ok()).toBeTruthy();
    const { token } = await loginApiRes.json();
    const authHeaders = { Authorization: `Bearer ${token}` };

    const existingRoutesRes = await request.get(`${baseURL}/api/routes`, { headers: authHeaders });
    if (existingRoutesRes.ok()) {
      const existingRoutes = await existingRoutesRes.json();
      for (const r of existingRoutes) {
        if (
          [
            '/mobile-banking/login',
            '/return-redirect',
            '/return-final',
            '/new-path',
            '/return-url-final-destination',
            '/return-url-redirector',
            '/mobile-banking-login',
          ].includes(r.path)
        ) {
          await request.delete(`${baseURL}/api/routes/${r.id}`, { headers: authHeaders });
        }
      }
    }

    const context = await browser.newContext();
    const uiPage = await context.newPage();

    // Sign in to Dynamic Mock UI
    await uiPage.goto(`${baseURL}/login`);
    await uiPage.locator('#login-username').fill('admin');
    await uiPage.locator('#login-password').fill('admin');
    await uiPage.locator('#login-submit').click();
    await expect(uiPage.locator('button[title="Add new stub"]')).toBeVisible({ timeout: 15000 });

    // Helper to create and configure a stub entirely through the Dynamic Mock UI
    async function configureStubInUI(options: {
      name: string;
      path: string;
      status: number;
      rawHeaders?: string;
      template: string;
      postScript?: string;
    }) {
      await uiPage.locator('button[title="Add new stub"]').click();
      const promptInput = uiPage.locator('input[placeholder="Route Name"]');
      await expect(promptInput).toBeVisible();
      await promptInput.fill(options.name);
      await uiPage.locator('button[type="submit"]:has-text("Save")').click();
      await expect(promptInput).toBeHidden();

      const slug = options.name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'new-path';

      // Wait for newly created stub to become active in RequestPanel
      const urlInput = uiPage.locator('input[placeholder="{{BASE_URL}}/api/example"]');
      await expect(urlInput).toBeVisible();
      await expect(urlInput).toHaveValue(`{{BASE_URL}}/${slug}`, { timeout: 10000 });
      await urlInput.fill(`{{BASE_URL}}${options.path}`);

      // Switch to Mock Stub Definition view
      await uiPage.locator('button:has-text("Mock Stub Definition")').click();

      // Configure Response Status & Headers
      await uiPage.locator('button:has-text("Response Status & Headers")').click();
      const statusInput = uiPage.locator('input[type="number"][min="100"][max="599"]');
      await statusInput.fill(String(options.status));

      if (options.rawHeaders) {
        await uiPage.locator('button[title="Raw / Bulk text view"]').click();
        const rawHeadersTextarea = uiPage.locator('textarea[placeholder*="Content-Type: application/json"]');
        await rawHeadersTextarea.fill(options.rawHeaders);
      }

      // Configure Response Template
      await uiPage.locator('button:has-text("Response Template")').click();
      await expect(uiPage.locator('.monaco-editor').first()).toBeVisible({ timeout: 15000 });
      await uiPage.waitForFunction(() => (window as any).monaco && (window as any).monaco.editor.getModels().length >= 1);
      await uiPage.evaluate((tpl) => {
        const models = (window as any).monaco.editor.getModels();
        if (models.length > 0) {
          models[0].setValue(tpl);
        }
      }, options.template);

      // Configure Polyglot Script if provided
      if (options.postScript) {
        await uiPage.locator('button:has-text("Polyglot Scripts")').click();
        await expect(uiPage.locator('.monaco-editor').nth(1)).toBeVisible({ timeout: 15000 });
        await uiPage.waitForFunction(() => (window as any).monaco && (window as any).monaco.editor.getModels().length >= 2);
        await uiPage.evaluate((script) => {
          const models = (window as any).monaco.editor.getModels();
          // Model 0 is Pre-Request Script, Model 1 is Post-Response Script
          if (models.length >= 2) {
            models[1].setValue(script);
          }
        }, options.postScript);
      }

      // Click Save in the UI
      await uiPage.locator('button[title="Save route configuration (⌘+S)"]').click();
      await expect(uiPage.locator('button[title="Save route configuration (⌘+S)"]')).toContainText('Saved', { timeout: 10000 });
      // Wait for Save button text to settle so subsequent saves can detect 'Saved'
      await expect(uiPage.locator('button[title="Save route configuration (⌘+S)"]')).toContainText('Save', { timeout: 5000 });
    }

    // 1A. Configure the final destination stub (/return-final) in the UI
    await configureStubInUI({
      name: 'Return URL Final Destination',
      path: '/return-final',
      status: 200,
      rawHeaders: 'Content-Type: application/json',
      template: '{"status":"redirect_followed_ok","finalDestination":true}',
    });

    // 1B. Configure the redirecting return URL stub (/return-redirect -> 302 Location: /mock/return-final) in the UI
    await configureStubInUI({
      name: 'Return URL Redirector',
      path: '/return-redirect',
      status: 302,
      rawHeaders: 'Location: http://localhost:8080/mock/return-final\nContent-Type: application/json',
      template: '{"status":"redirecting"}',
    });

    // 1C. STEP 1: Configure the Mobile Banking Login stub (/mobile-banking/login) in the UI
    await configureStubInUI({
      name: 'Mobile Banking Login',
      path: '/mobile-banking/login?returnUrl=http://localhost/mock/return-redirect',
      status: 200,
      rawHeaders: 'Content-Type: text/html; charset=utf-8',
      template: STANDALONE_LOGIN_HTML,
      postScript: POLYGLOT_POST_SCRIPT,
    });

    // Verify URL persistence across Logout and Relogin
    const urlInput = uiPage.locator('input[placeholder="{{BASE_URL}}/api/example"]');
    await expect(urlInput).toHaveValue('{{BASE_URL}}/mobile-banking/login?returnUrl=http://localhost/mock/return-redirect');
    await uiPage.locator('#header-user-menu-btn').click();
    await uiPage.locator('#header-logout-btn').click();
    await expect(uiPage.locator('#login-username')).toBeVisible({ timeout: 10000 });
    await uiPage.locator('#login-username').fill('admin');
    await uiPage.locator('#login-password').fill('admin');
    await uiPage.locator('#login-submit').click();
    await expect(uiPage.locator('button[title="Add new stub"]')).toBeVisible({ timeout: 15000 });
    await expect(urlInput).toHaveValue('{{BASE_URL}}/mobile-banking/login?returnUrl=http://localhost/mock/return-redirect');

    // Clear the Live Traffic Console in the UI before testing
    const consolePanel = uiPage.locator('div.bg-black.text-green-500');
    await expect(consolePanel).toBeVisible();
    const clearConsoleBtn = uiPage.locator('button:has-text("Clear")');
    if (await clearConsoleBtn.isVisible()) {
      await clearConsoleBtn.click();
    }

    // =========================================================================
    // STEP 2: Open the mock endpoint (with returnUrl)
    // =========================================================================
    const consumerPage = await context.newPage();
    const configuredReturnUrl = `${baseURL}/mock/return-redirect`;
    const openResponse = await consumerPage.goto(
      `${baseURL}/mock/mobile-banking/login?returnUrl=${encodeURIComponent(configuredReturnUrl)}`
    );

    expect(openResponse).not.toBeNull();
    expect(openResponse!.status()).toBe(200);
    // Verify response is rendered as HTML rather than JSON
    const contentType = openResponse!.headers()['content-type'] || '';
    expect(contentType).toContain('text/html');
    expect(contentType).not.toContain('application/json');

    // Verify custom mobile-banking login page elements and styling are rendered in the DOM
    await expect(consumerPage.locator('#mobile-banking-login-card')).toBeVisible();
    await expect(consumerPage.locator('h1')).toHaveText('Secure Sign In');
    await expect(consumerPage.locator('#mobile-number')).toBeVisible();
    await expect(consumerPage.locator('#mpin')).toBeVisible();
    await expect(consumerPage.locator('#login-btn')).toHaveText('Login');

    const cardBgColor = await consumerPage.locator('#mobile-banking-login-card').evaluate(
      (el) => window.getComputedStyle(el).backgroundColor
    );
    expect(cardBgColor).toBe('rgb(30, 41, 59)');

    // CRITICAL DISTINCTION: Rendering the login page MUST NOT call the return URL!
    await consumerPage.waitForTimeout(1000);
    await expect(consolePanel.locator('span:has-text("/mobile-banking/login")').first()).toBeVisible();
    await expect(consolePanel.locator('span:has-text("/return-redirect")')).toHaveCount(0);
    await expect(consolePanel.locator('span:has-text("/return-final")')).toHaveCount(0);
    await expect(consolePanel.locator('span:has-text("WEBHOOK")')).toHaveCount(0);

    // Also verify that returnUrl in request body is supported without calling returnUrl on render
    const bodyRenderRes = await consumerPage.request.fetch(`${baseURL}/mock/mobile-banking/login`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      data: JSON.stringify({ returnUrl: configuredReturnUrl }),
    });
    expect(bodyRenderRes.status()).toBe(200);
    expect(bodyRenderRes.headers()['content-type']).toContain('text/html');
    const bodyRenderHtml = await bodyRenderRes.text();
    expect(bodyRenderHtml).toContain(`data-return-url="${configuredReturnUrl}"`);
    await consumerPage.waitForTimeout(500);
    await expect(consolePanel.locator('span:has-text("/return-redirect")')).toHaveCount(0);
    await expect(consolePanel.locator('span:has-text("/return-final")')).toHaveCount(0);

    // =========================================================================
    // STEP 3: Click Login
    // Only this action should trigger the backend to call the configured return URL.
    // Since /mock/return-redirect responds with 302 -> /mock/return-final, the backend
    // must follow the redirect to /mock/return-final.
    // =========================================================================
    await consumerPage.locator('#login-btn').click();
    await expect(consumerPage.locator('#login-status')).toHaveText('Login completed - return URL triggered');

    // Verify in the UI Live Traffic Console that:
    // 1. The backend called /return-redirect (302)
    // 2. The backend followed the redirect to /return-final (200)
    // 3. The async WEBHOOK event completed with status 200
    await expect(consolePanel.locator('span:has-text("/return-redirect")').first()).toBeVisible({ timeout: 10000 });
    await expect(consolePanel.locator('span:has-text("/return-final")').first()).toBeVisible({ timeout: 10000 });
    await expect(consolePanel.locator('span:has-text("WEBHOOK")').first()).toBeVisible({ timeout: 10000 });

    // =========================================================================
    // STEP 4: Test without a return URL
    // The HTML response should display exactly: "no return url were provided"
    // No return-URL request should be made.
    // =========================================================================
    await uiPage.locator('button:has-text("Clear")').click();
    await expect(consolePanel.locator('span:has-text("/return-redirect")')).toHaveCount(0);

    const noReturnUrlRes = await consumerPage.goto(`${baseURL}/mock/mobile-banking/login`);
    expect(noReturnUrlRes).not.toBeNull();
    expect(noReturnUrlRes!.status()).toBe(200);
    expect(noReturnUrlRes!.headers()['content-type']).toContain('text/html');

    const rawNoReturnText = await noReturnUrlRes!.text();
    expect(rawNoReturnText).toBe('no return url were provided');
    await expect(consumerPage.locator('body')).toHaveText('no return url were provided');

    // Wait and confirm NO return-URL request or webhook was made
    await consumerPage.waitForTimeout(1000);
    await expect(consolePanel.locator('span:has-text("/mobile-banking/login")').first()).toBeVisible();
    await expect(consolePanel.locator('span:has-text("/return-redirect")')).toHaveCount(0);
    await expect(consolePanel.locator('span:has-text("/return-final")')).toHaveCount(0);
    await expect(consolePanel.locator('span:has-text("WEBHOOK")')).toHaveCount(0);

    await context.close();
  });
});

