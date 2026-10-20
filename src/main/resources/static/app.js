const appEl = document.getElementById('app');
const navEl = document.getElementById('nav');
const userBoxEl = document.getElementById('user-box');

// Which tabs each role sees. We will add more tabs here later.
const NAV = {
    customer: [['paintings', 'Paintings'], ['orders', 'My orders'], ['bookings', 'Wall bookings'], ['pottery', 'Pottery']],
    studio: [['paintings', 'Paintings'], ['orders', 'All orders'], ['bookings', 'Wall bookings'], ['pottery', 'Pottery']]
};
// Which function draws which page. We will add more pages here later.
const views = {
    login: renderLogin,
    signup: renderSignup,
    paintings: renderPaintings,
    orders: renderOrders,
    bookings: renderBookings,
    pottery: renderPottery
};

// ---------- Routing: decide what to show based on the #/... part of the URL ----------
function route() {
    const session = getSession();
    const name = location.hash.replace('#/', '');
    const publicViews = ['login', 'signup'];

    if (!session) {
        if (!publicViews.includes(name)) {
            location.hash = '#/login';
            return;
        }
    } else if (!views[name] || publicViews.includes(name)) {
        location.hash = '#/paintings';
        return;
    }

    renderHeader(session, name);
    views[name](appEl);
}

function renderHeader(session, current) {
    if (!session) {
        navEl.innerHTML = '';
        userBoxEl.innerHTML = '';
        return;
    }
    navEl.innerHTML = NAV[session.user.role].map(([id, label]) =>
        '<a href="#/' + id + '" class="' + (id === current ? 'active' : '') + '">' + esc(label) + '</a>'
    ).join('');

    userBoxEl.innerHTML =
        '<span>' + esc(session.user.name) + ' (' + esc(session.user.role) + ')</span>' +
        '<button type="button" id="logout">Log out</button>';
    document.getElementById('logout').addEventListener('click', () => {
        clearSession();
        location.hash = '#/login';
    });
}

// ---------- Login and signup ----------
function renderLogin(root) {
    root.innerHTML = `
        <section class="card form-card">
            <h2>Log in</h2>
            <form id="login-form">
                <label>Email <input type="email" name="email" required autocomplete="username"></label>
                <label>Password <input type="password" name="password" required autocomplete="current-password"></label>
                <button type="submit">Log in</button>
            </form>
            <p>New customer? <a href="#/signup">Create an account</a></p>
        </section>`;

    document.getElementById('login-form').addEventListener('submit', async (event) => {
        event.preventDefault();
        const form = event.target;
        const button = form.querySelector('button');
        const values = Object.fromEntries(new FormData(form));
        button.disabled = true;
        try {
            const data = await api('POST', '/api/auth/login',
                { email: values.email, password: values.password }, { noAuth: true });
            saveSession(data);
            location.hash = '#/paintings';
        } catch (err) {
            showBanner(err.message);
        } finally {
            button.disabled = false;
        }
    });
}

function renderSignup(root) {
    root.innerHTML = `
        <section class="card form-card">
            <h2>Create a customer account</h2>
            <form id="signup-form">
                <label>Name <input type="text" name="name" required maxlength="100"></label>
                <label>Email <input type="email" name="email" required maxlength="150"></label>
                <label>Password (at least 6 characters)
                    <input type="password" name="password" required minlength="6" maxlength="100" autocomplete="new-password"></label>
                <button type="submit">Sign up</button>
            </form>
            <p>Already have an account? <a href="#/login">Log in</a></p>
        </section>`;

    document.getElementById('signup-form').addEventListener('submit', async (event) => {
        event.preventDefault();
        const form = event.target;
        const button = form.querySelector('button');
        const values = Object.fromEntries(new FormData(form));
        button.disabled = true;
        try {
            const data = await api('POST', '/api/auth/signup',
                { name: values.name, email: values.email, password: values.password }, { noAuth: true });
            saveSession(data);
            location.hash = '#/paintings';
        } catch (err) {
            showBanner(err.message);
        } finally {
            button.disabled = false;
        }
    });
}

// ---------- Paintings ----------
function renderPaintings(root) {
    const isStudio = getSession().user.role === 'studio';
    let current = [];   // the paintings currently on screen

    root.innerHTML = `
        <div class="page-head">
            <h2>Paintings</h2>
            ${isStudio ? '<button type="button" id="add-btn">Add painting</button>' : ''}
        </div>
        <div id="painting-form-slot"></div>
        <div id="painting-list"></div>`;

    const listEl = root.querySelector('#painting-list');
    const formSlot = root.querySelector('#painting-form-slot');

    function reload() {
        return loadInto(listEl, () => api('GET', '/api/paintings'), showGrid, 'There are no paintings yet.');
    }

    function showGrid(paintings) {
        current = paintings;
        listEl.innerHTML = '<div class="grid">' + paintings.map(card).join('') + '</div>';

        listEl.querySelectorAll('[data-buy]').forEach(btn =>
            btn.addEventListener('click', () => buy(btn)));
        listEl.querySelectorAll('[data-edit]').forEach(btn =>
            btn.addEventListener('click', () =>
                openForm(current.find(p => p.id === btn.dataset.edit))));
    }

    function card(p) {
        const sold = p.status !== 'available';
        let action;
        if (sold) {
            action = '<span class="badge sold">Sold</span>';
        } else if (isStudio) {
            action = '<span class="badge available">Available</span>' +
                '<button type="button" data-edit="' + esc(p.id) + '">Edit</button>';
        } else {
            action = '<span class="badge available">Available</span>' +
                '<button type="button" data-buy="' + esc(p.id) + '">Buy</button>';
        }
        return `
            <article class="card painting ${sold ? 'is-sold' : ''}">
                ${p.imageUrl ? '<img src="' + esc(p.imageUrl) + '" alt="' + esc(p.title) + '" onerror="this.style.display=\'none\'">' : ''}
                <h3>${esc(p.title)}</h3>
                <p class="muted">by ${esc(p.artist)}</p>
                <p class="desc">${esc(p.description)}</p>
                <p class="price">${formatPrice(p.price)}</p>
                <div class="actions">${action}</div>
            </article>`;
    }

    async function buy(btn) {
        const painting = current.find(p => p.id === btn.dataset.buy);
        if (!confirm('Buy "' + painting.title + '" for ' + formatPrice(painting.price) + '?')) {
            return;
        }
        btn.disabled = true;
        try {
            await api('POST', '/api/orders', { paintingId: painting.id });
            showBanner('Order placed! You can see it under My orders.', 'success');
        } catch (err) {
            showBanner(err.message);   // for example: "Sorry, this painting was just sold"
        }
        reload();   // always refresh, so the screen matches reality
    }

    function openForm(painting) {
        const editing = Boolean(painting);
        formSlot.innerHTML = `
            <section class="card form-card">
                <h3>${editing ? 'Edit painting ' + esc(painting.id) : 'Add painting'}</h3>
                <form id="painting-form">
                    <label>Title <input name="title" required maxlength="300" value="${esc(painting?.title)}"></label>
                    <label>Artist <input name="artist" required maxlength="100" value="${esc(painting?.artist)}"></label>
                    <label>Description <textarea name="description" maxlength="2000">${esc(painting?.description)}</textarea></label>
                    <label>Price (INR) <input name="price" type="number" step="0.01" min="0.01" required value="${esc(painting?.price)}"></label>
                    <label>Image path <input name="imageUrl" maxlength="255" placeholder="images/p-001.jpg" value="${esc(painting?.imageUrl)}"></label>
                    <button type="submit">Save</button>
                    <button type="button" id="cancel-form">Cancel</button>
                </form>
            </section>`;
        formSlot.scrollIntoView({ behavior: 'smooth' });

        formSlot.querySelector('#cancel-form').addEventListener('click', () => {
            formSlot.innerHTML = '';
        });

        formSlot.querySelector('#painting-form').addEventListener('submit', async (event) => {
            event.preventDefault();
            const form = event.target;
            const values = Object.fromEntries(new FormData(form));
            const body = {
                title: values.title,
                artist: values.artist,
                description: values.description,
                price: Number(values.price),
                imageUrl: values.imageUrl.trim() || null
            };
            const button = form.querySelector('button[type="submit"]');
            button.disabled = true;
            try {
                if (editing) {
                    await api('PUT', '/api/paintings/' + painting.id, body);
                } else {
                    await api('POST', '/api/paintings', body);
                }
                showBanner(editing ? 'Painting updated.' : 'Painting added.', 'success');
                formSlot.innerHTML = '';
            } catch (err) {
                showBanner(err.message);
                button.disabled = false;
            }
            reload();
        });
    }

    const addBtn = root.querySelector('#add-btn');
    if (addBtn) {
        addBtn.addEventListener('click', () => openForm(null));
    }

    reload();
}

// ---------- Orders ----------
function renderOrders(root) {
    const isStudio = getSession().user.role === 'studio';

    root.innerHTML = `
        <div class="page-head"><h2>${isStudio ? 'All orders' : 'My orders'}</h2></div>
        <div id="order-list"></div>`;
    const listEl = root.querySelector('#order-list');

    function reload() {
        const emptyText = isStudio
            ? 'No orders yet.'
            : 'You have not bought any paintings yet. Open the Paintings tab to get started.';
        return loadInto(listEl, () => api('GET', '/api/orders'), showTable, emptyText);
    }

    function showTable(orders) {
        listEl.innerHTML = `
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>#</th><th>Painting</th>
                            ${isStudio ? '<th>Customer</th>' : ''}
                            <th>Price</th><th>Status</th><th>Placed on</th><th></th>
                        </tr>
                    </thead>
                    <tbody>${orders.map(row).join('')}</tbody>
                </table>
            </div>`;
        listEl.querySelectorAll('[data-cancel]').forEach(btn =>
            btn.addEventListener('click', () => cancel(btn)));
    }

    function row(o) {
        const canCancel = !isStudio && o.status === 'placed';
        return `
            <tr>
                <td>${o.id}</td>
                <td>${esc(o.paintingTitle)}</td>
                ${isStudio ? '<td>' + esc(o.customerName) + '<br><span class="muted">' + esc(o.customerEmail) + '</span></td>' : ''}
                <td>${formatPrice(o.price)}</td>
                <td><span class="badge ${esc(o.status)}">${esc(o.status)}</span></td>
                <td>${esc(formatDateTime(o.createdAt))}</td>
                <td>${canCancel ? '<button type="button" data-cancel="' + o.id + '">Cancel</button>' : ''}</td>
            </tr>`;
    }

    async function cancel(btn) {
        if (!confirm('Cancel this order? The painting will become available again.')) {
            return;
        }
        btn.disabled = true;
        try {
            await api('POST', '/api/orders/' + btn.dataset.cancel + '/cancel');
            showBanner('Order cancelled.', 'success');
        } catch (err) {
            showBanner(err.message);
        }
        reload();
    }

    reload();
}

// ---------- Wall bookings ----------
// Only used for the price preview. The server calculates the real price.
const MURAL_RATE_PREVIEW = 150;

function renderBookings(root) {
    const isStudio = getSession().user.role === 'studio';

    root.innerHTML = `
        <div class="page-head"><h2>${isStudio ? 'Wall booking requests' : 'Wall bookings'}</h2></div>
        ${isStudio ? '' : `
        <section class="card booking-form">
            <h3>Book a wall painting</h3>
            <form id="booking-form">
                <label>Date (7 to 90 days from today)
                    <input type="date" name="date" required min="${dateInDays(7)}" max="${dateInDays(90)}"></label>
                <label>Wall size in sq ft (10 to 500)
                    <input type="number" name="wallSize" required min="10" max="500" step="1"></label>
                <label>Address
                    <textarea name="address" required maxlength="300"></textarea></label>
                <p class="price-preview" id="price-preview"></p>
                <button type="submit">Request booking</button>
            </form>
        </section>`}
        <div id="booking-list"></div>`;

    const listEl = root.querySelector('#booking-list');
    const form = root.querySelector('#booking-form');
    const previewEl = root.querySelector('#price-preview');

    function reload() {
        const emptyText = isStudio
            ? 'No booking requests yet.'
            : 'You have no wall bookings yet. Use the form above to request one.';
        return loadInto(listEl, () => api('GET', '/api/bookings'), showTable, emptyText);
    }

    function showTable(bookings) {
        listEl.innerHTML = `
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Date</th>
                            ${isStudio ? '<th>Customer</th>' : ''}
                            <th>Wall size</th><th>Address</th><th>Price</th><th>Status</th><th></th>
                        </tr>
                    </thead>
                    <tbody>${bookings.map(row).join('')}</tbody>
                </table>
            </div>`;

        listEl.querySelectorAll('[data-confirm]').forEach(btn =>
            btn.addEventListener('click', () =>
                act(btn, '/api/bookings/' + btn.dataset.confirm + '/confirm', 'Booking confirmed.')));
        listEl.querySelectorAll('[data-decline]').forEach(btn =>
            btn.addEventListener('click', () =>
                act(btn, '/api/bookings/' + btn.dataset.decline + '/decline', 'Booking declined.',
                    'Decline this booking? The date will become free for others.')));
        listEl.querySelectorAll('[data-cancel]').forEach(btn =>
            btn.addEventListener('click', () =>
                act(btn, '/api/bookings/' + btn.dataset.cancel + '/cancel', 'Booking cancelled.',
                    'Cancel this booking? The date will become free for others.')));
    }

    function row(b) {
        const active = b.status === 'requested' || b.status === 'confirmed';
        let actions = '';
        if (isStudio) {
            if (b.status === 'requested') {
                actions = '<button type="button" data-confirm="' + b.id + '">Confirm</button> ' +
                    '<button type="button" data-decline="' + b.id + '">Decline</button>';
            }
        } else if (active) {
            // This is only a hint for the user. The server checks the 3-day rule again.
            actions = daysUntil(b.date) >= 3
                ? '<button type="button" data-cancel="' + b.id + '">Cancel</button>'
                : '<span class="muted">Cannot cancel within 3 days</span>';
        }
        return `
            <tr>
                <td>${esc(formatDate(b.date))}</td>
                ${isStudio ? '<td>' + esc(b.customerName) + '<br><span class="muted">' + esc(b.customerEmail) + '</span></td>' : ''}
                <td>${b.wallSize} sq ft</td>
                <td>${esc(b.address)}</td>
                <td>${formatPrice(b.price)}</td>
                <td><span class="badge ${esc(b.status)}">${esc(b.status)}</span></td>
                <td>${actions}</td>
            </tr>`;
    }

    // One helper for every button in the table: confirm, call the API, show the result, refresh.
    async function act(btn, path, successMessage, confirmText) {
        if (confirmText && !confirm(confirmText)) {
            return;
        }
        btn.disabled = true;
        try {
            await api('POST', path);
            showBanner(successMessage, 'success');
        } catch (err) {
            showBanner(err.message);
        }
        reload();
    }

    function updatePreview() {
        const size = Number(form.wallSize.value);
        previewEl.textContent = size >= 10 && size <= 500
            ? 'Estimated price: ' + formatPrice(size * MURAL_RATE_PREVIEW)
            : 'Enter a wall size to see the estimated price.';
    }

    if (form) {
        updatePreview();
        form.wallSize.addEventListener('input', updatePreview);

        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const values = Object.fromEntries(new FormData(form));
            const button = form.querySelector('button[type="submit"]');
            button.disabled = true;
            try {
                await api('POST', '/api/bookings', {
                    date: values.date,
                    wallSize: Number(values.wallSize),
                    address: values.address
                });
                showBanner('Booking requested! The studio will confirm or decline it.', 'success');
                form.reset();
                updatePreview();
            } catch (err) {
                showBanner(err.message);   // for example: "Sorry, this date was just taken"
            } finally {
                button.disabled = false;
            }
            reload();
        });
    }

    reload();
}

// ---------- Custom pottery ----------
function renderPottery(root) {
    const isStudio = getSession().user.role === 'studio';
    let options = null;          // items and glazes from the server
    let currentRequests = [];    // the requests currently on screen

    root.innerHTML = `
        <div class="page-head"><h2>${isStudio ? 'Pottery requests' : 'Custom pottery'}</h2></div>
        ${isStudio ? '' : '<div id="pottery-form-slot"></div>'}
        <div id="pottery-list"></div>`;

    const listEl = root.querySelector('#pottery-list');
    const formSlot = root.querySelector('#pottery-form-slot');   // does not exist for the studio

    // ----- The list of requests -----
    function reload() {
        const emptyText = isStudio
            ? 'No pottery requests yet.'
            : 'You have no pottery requests yet. Use the form above to send one.';
        return loadInto(listEl, () => api('GET', '/api/pottery'), showTable, emptyText);
    }

    function showTable(requests) {
        currentRequests = requests;
        listEl.innerHTML = `
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>#</th><th>Item</th><th>Glaze</th><th>Quantity</th><th>Needed by</th>
                            ${isStudio ? '<th>Customer</th>' : ''}
                            <th>Status</th><th></th>
                        </tr>
                    </thead>
                    <tbody>${requests.map(row).join('')}</tbody>
                </table>
            </div>`;

        listEl.querySelectorAll('[data-edit]').forEach(btn =>
            btn.addEventListener('click', () => {
                const request = currentRequests.find(r => String(r.id) === btn.dataset.edit);
                if (request) {
                    drawForm(request);
                    formSlot.scrollIntoView({ behavior: 'smooth' });
                }
            }));
        listEl.querySelectorAll('[data-cancel]').forEach(btn =>
            btn.addEventListener('click', () =>
                act(btn, '/api/pottery/' + btn.dataset.cancel + '/cancel', 'Request cancelled.',
                    'Cancel this pottery request?')));
        listEl.querySelectorAll('[data-accept]').forEach(btn =>
            btn.addEventListener('click', () =>
                act(btn, '/api/pottery/' + btn.dataset.accept + '/accept', 'Request accepted.')));
        listEl.querySelectorAll('[data-decline]').forEach(btn =>
            btn.addEventListener('click', () =>
                act(btn, '/api/pottery/' + btn.dataset.decline + '/decline', 'Request declined.',
                    'Decline this pottery request?')));
    }

    function row(r) {
        let actions = '';
        if (r.status === 'requested') {
            actions = isStudio
                ? '<button type="button" data-accept="' + r.id + '">Accept</button> ' +
                '<button type="button" data-decline="' + r.id + '">Decline</button>'
                : '<button type="button" data-edit="' + r.id + '">Edit</button> ' +
                '<button type="button" data-cancel="' + r.id + '">Cancel</button>';
        }
        return `
            <tr>
                <td>${r.id}</td>
                <td>${esc(r.itemName)}</td>
                <td>${esc(r.glazeName)}</td>
                <td>${r.quantity}</td>
                <td>${esc(formatDate(r.neededBy))}</td>
                ${isStudio ? '<td>' + esc(r.customerName) + '<br><span class="muted">' + esc(r.customerEmail) + '</span></td>' : ''}
                <td><span class="badge ${esc(r.status)}">${esc(r.status)}</span></td>
                <td>${actions}</td>
            </tr>`;
    }

    // One helper for the buttons in the table: confirm, call the API, show the result, refresh.
    async function act(btn, path, successMessage, confirmText) {
        if (confirmText && !confirm(confirmText)) {
            return;
        }
        btn.disabled = true;
        try {
            await api('POST', path);
            showBanner(successMessage, 'success');
        } catch (err) {
            showBanner(err.message);   // for example: "...can no longer be cancelled..."
        }
        reload();
    }

    // ----- The form (customers only). editing = null for a new request, or the request being edited -----
    function drawForm(editing) {
        const optionTags = (list, selectedId) => list.map(o =>
            '<option value="' + esc(o.id) + '"' + (o.id === selectedId ? ' selected' : '') + '>' +
            esc(o.name) + '</option>').join('');

        formSlot.innerHTML = `
            <section class="card booking-form">
                <h3>${editing ? 'Edit request #' + editing.id : 'Request custom pottery'}</h3>
                <form id="pottery-form">
                    <label>Item
                        <select name="itemId" required>
                            <option value="">Choose an item</option>
                            ${optionTags(options.items, editing ? editing.itemId : null)}
                        </select></label>
                    <label>Glaze
                        <select name="glazeId" required>
                            <option value="">Choose a glaze</option>
                            ${optionTags(options.glazes, editing ? editing.glazeId : null)}
                        </select></label>
                    <label>Quantity (1 to 12)
                        <input type="number" name="quantity" required min="1" max="12" step="1"
                               value="${editing ? editing.quantity : ''}"></label>
                    <label>Needed by (at least 14 days from today)
                        <input type="date" name="neededBy" required min="${dateInDays(14)}"
                               value="${editing ? esc(editing.neededBy) : ''}"></label>
                    <button type="submit">${editing ? 'Save changes' : 'Send request'}</button>
                    ${editing ? '<button type="button" id="cancel-edit">Cancel edit</button>' : ''}
                </form>
            </section>`;

        const form = formSlot.querySelector('#pottery-form');

        if (editing) {
            formSlot.querySelector('#cancel-edit').addEventListener('click', () => drawForm(null));
        }

        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const values = Object.fromEntries(new FormData(form));
            const body = {
                itemId: values.itemId,
                glazeId: values.glazeId,
                quantity: Number(values.quantity),
                neededBy: values.neededBy
            };
            const button = form.querySelector('button[type="submit"]');
            button.disabled = true;
            try {
                if (editing) {
                    await api('PUT', '/api/pottery/' + editing.id, body);
                    showBanner('Request updated.', 'success');
                } else {
                    await api('POST', '/api/pottery', body);
                    showBanner('Request sent! The studio will accept or decline it.', 'success');
                }
                drawForm(null);   // back to an empty "new request" form
            } catch (err) {
                showBanner(err.message);
                button.disabled = false;
            }
            reload();
        });
    }

    // ----- Start: load the dropdown options (customers only), then the list -----
    if (formSlot) {
        loadInto(formSlot,
            () => api('GET', '/api/pottery/options'),
            (data) => {
                options = data;
                drawForm(null);
            },
            '');
    }
    reload();
}

// ---------- Start ----------
window.addEventListener('hashchange', route);
route();
