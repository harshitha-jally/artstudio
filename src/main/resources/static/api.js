// ---------- Session (who is logged in) ----------
const SESSION_KEY = 'artstudio_session';

function getSession() {
    try {
        return JSON.parse(localStorage.getItem(SESSION_KEY));
    } catch (e) {
        return null;
    }
}

function saveSession(data) {
    localStorage.setItem(SESSION_KEY, JSON.stringify({
        token: data.token,
        user: { name: data.name, email: data.email, role: data.role }
    }));
}

function clearSession() {
    localStorage.removeItem(SESSION_KEY);
}

// ---------- Small helpers ----------
// Makes text safe to put inside HTML, so nobody can inject <script> through a title or address.
function esc(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function formatPrice(amount) {
    return '\u20B9' + Number(amount).toLocaleString('en-IN');
}

function formatDateTime(iso) {
    return new Date(iso).toLocaleString('en-IN');
}

// ---------- Messages at the top of the page ----------
let bannerTimer;

function showBanner(message, type = 'error') {
    const banner = document.getElementById('banner');
    banner.textContent = message;
    banner.className = 'banner ' + type;
    clearTimeout(bannerTimer);
    bannerTimer = setTimeout(hideBanner, 6000);
}

function hideBanner() {
    document.getElementById('banner').className = 'banner hidden';
}

// ---------- Talking to the server ----------
// Every call to the backend goes through this one function.
async function api(method, path, body, options = {}) {
    const session = getSession();
    const headers = {};
    if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
    }
    if (session && !options.noAuth) {
        headers['Authorization'] = 'Bearer ' + session.token;
    }

    let response;
    try {
        response = await fetch(path, {
            method: method,
            headers: headers,
            body: body !== undefined ? JSON.stringify(body) : undefined
        });
    } catch (e) {
        throw new Error('Cannot reach the server. Check that the app is running and try again.');
    }

    let data = null;
    const text = await response.text();
    if (text) {
        try {
            data = JSON.parse(text);
        } catch (e) {
            data = null;
        }
    }

    if (!response.ok) {
        // Token expired or invalid: send the user back to login.
        if (response.status === 401 && session && !options.noAuth) {
            clearSession();
            location.hash = '#/login';
            throw new Error('Your session expired. Please log in again.');
        }
        throw new Error((data && data.message) || 'Something went wrong (status ' + response.status + ')');
    }
    return data;
}

// ---------- Loading, empty and error states in one place ----------
// container: the element to fill
// fetcher:   a function that returns a promise with the data
// render:    a function that draws the data
// emptyText: what to say when the list is empty
async function loadInto(container, fetcher, render, emptyText) {
    container.innerHTML = '<p class="state">Loading...</p>';
    try {
        const data = await fetcher();
        if (Array.isArray(data) && data.length === 0) {
            container.innerHTML = '<p class="state">' + esc(emptyText) + '</p>';
            return;
        }
        render(data, container);
    } catch (err) {
        container.innerHTML =
            '<div class="state error"><p>' + esc(err.message) + '</p>' +
            '<button type="button" class="retry">Try again</button></div>';
        container.querySelector('.retry').addEventListener('click',
            () => loadInto(container, fetcher, render, emptyText));
    }
}