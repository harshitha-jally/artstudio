const appEl = document.getElementById('app');
const navEl = document.getElementById('nav');
const userBoxEl = document.getElementById('user-box');

// Which tabs each role sees. We will add more tabs here later.
const NAV = {
    customer: [['paintings', 'Paintings'], ['orders', 'My orders']],
    studio: [['paintings', 'Paintings'], ['orders', 'All orders']]
};

// Which function draws which page. We will add more pages here later.
const views = {
    login: renderLogin,
    signup: renderSignup,
    paintings: renderPaintings,
    orders: renderOrders
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

// ---------- Start ----------
window.addEventListener('hashchange', route);
route();