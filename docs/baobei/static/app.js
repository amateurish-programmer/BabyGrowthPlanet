(() => {
  'use strict';
  const byId = id => document.getElementById(id);
  const configured = window.BAOBEI_CONFIG?.apiBaseUrl;
  const prefix = typeof configured === 'string' ? configured.replace(/\/$/, '') : '';
  if (prefix !== '/baobei/api/v1' && !/^https:\/\/[a-z0-9][a-z0-9-]*\.supabase\.co\/functions\/v1\/baobei-api$/.test(prefix)) {
    byId('notice').textContent = '账号管理配置无效，请联系管理员。';
    byId('notice').hidden = false;
    byId('notice').classList.add('error');
    for (const button of document.querySelectorAll('form button')) button.disabled = true;
    return;
  }
  let token = '';
  let account = null;
  let sessionId = '';
  let sessionGeneration = 0;
  let pendingConfirmation = null;
  let confirming = false;
  const deviceId = (() => {
    const key = 'baobei.management.deviceId';
    try {
      const stored = localStorage.getItem(key);
      if (stored && /^[a-zA-Z0-9_-]{16,80}$/.test(stored)) return stored;
      const id = crypto.randomUUID();
      localStorage.setItem(key, id);
      return id;
    } catch (_) { return crypto.randomUUID(); }
  })();
  const knownErrors = new Set(['用户名或密码错误', '用户名已存在', '邀请码无效，请向管理员获取', '原密码错误', '密码需为10至128个字符', '用户名需为3至64个字符', '账号已禁用', '账号已禁用，请联系管理员', '不能禁用自己的账号', '不能重置自己的密码', '邀请码需为16至128个字母、数字、下划线或短横线']);
  const silent = () => Object.assign(new Error(), { silent: true });
  const notice = (message = '', error = false) => {
    byId('notice').textContent = message;
    byId('notice').classList.toggle('error', error);
    byId('notice').hidden = !message;
  };
  function resetSession(message = '') {
    sessionGeneration++;
    token = ''; account = null; sessionId = '';
    pendingConfirmation = null;
    byId('confirm-dialog').close();
    byId('account-view').hidden = true;
    byId('admin-view').hidden = true;
    byId('session-actions').hidden = true;
    byId('auth-view').hidden = false;
    for (const id of ['account-username', 'account-id', 'account-role', 'current-invite', 'users-count']) byId(id).textContent = '';
    byId('devices-list').replaceChildren(); byId('users-list').replaceChildren();
    for (const form of document.querySelectorAll('form')) form.reset();
    showAuth('login');
    notice(message);
  }
  function showAuth(mode) {
    const register = mode === 'register';
    byId('register-form').hidden = !register;
    byId('login-form').hidden = register;
    byId('auth-heading').textContent = register ? '注册家庭账号' : '登录家庭账号';
    for (const name of ['login', 'register']) {
      byId(`${name}-tab`).setAttribute('aria-pressed', String(name === mode));
      byId(`${name}-tab`).classList.toggle('secondary', name !== mode);
    }
    for (const input of document.querySelectorAll('#auth-view input[type=password]')) input.value = '';
  }
  async function api(path, method = 'GET', body) {
    const requestToken = token;
    const requestGeneration = sessionGeneration;
    const stale = () => requestGeneration !== sessionGeneration || requestToken !== token;
    const headers = { Accept: 'application/json' };
    if (prefix === '/baobei/api/v1') headers['ngrok-skip-browser-warning'] = 'true';
    if (requestToken) headers.Authorization = `Bearer ${requestToken}`;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    let response;
    try {
      response = await fetch(prefix + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body), credentials: 'omit', cache: 'no-store', redirect: 'error' });
    } catch (_) {
      if (stale()) throw silent();
      throw new Error('无法连接服务器，请检查网络后重试。App 本地功能可继续使用。');
    }
    if (stale()) throw silent();
    let data;
    try { data = await response.json(); }
    catch (_) { if (stale()) throw silent(); throw new Error('服务器响应异常，请稍后重试。'); }
    if (stale()) throw silent();
    if (!response.ok) {
      if (response.status === 401 && requestToken) {
        resetSession('登录已失效，请重新登录。');
        throw silent();
      }
      const fallback = {
        400: '输入内容无效，请检查后重试。', 401: '用户名或密码错误，或账号不可用。',
        403: '无权执行此操作，请检查邀请码或联系管理员。', 404: '操作对象不存在，请刷新后重试。',
        409: '信息已存在或发生冲突，请检查后重试。', 429: '尝试次数过多，请稍后重试。'
      };
      throw new Error(knownErrors.has(data?.detail) ? data.detail : fallback[response.status] || '家庭服务器暂时无法完成操作，请稍后重试。');
    }
    return data;
  }
  async function run(button, action) {
    if (button.disabled) return;
    button.disabled = true;
    notice();
    try { await action(); } catch (error) { if (!error.silent) notice(error.message, true); }
    finally { button.disabled = false; }
  }
  const node = (tag, text, className) => {
    const element = document.createElement(tag);
    if (text !== undefined) element.textContent = String(text ?? '');
    if (className) element.className = className;
    return element;
  };
  const date = value => {
    const time = Number(value);
    if (!time || !Number.isFinite(time)) return '未记录';
    const parsed = new Date(time);
    return Number.isNaN(parsed.getTime()) ? '未记录' : parsed.toLocaleString('zh-CN', { hour12: false });
  };
  function button(text, action, disabled = false) {
    const element = node('button', text, 'secondary');
    element.type = 'button'; element.disabled = disabled;
    element.addEventListener('click', action);
    return element;
  }
  function confirmAction(title, description, action, password = false) {
    pendingConfirmation = action;
    byId('confirm-form').reset();
    byId('confirm-title').textContent = title;
    byId('confirm-description').textContent = description;
    byId('confirm-error').hidden = true;
    byId('confirm-error').textContent = '';
    byId('confirm-password-fields').hidden = !password;
    byId('confirm-password').required = password;
    byId('confirm-password-repeat').required = password;
    byId('confirm-password').setCustomValidity('');
    byId('confirm-password-repeat').setCustomValidity('');
    byId('confirm-dialog').showModal();
    byId(password ? 'confirm-password' : 'confirm-cancel').focus();
  }
  function renderDevices(devices) {
    const list = byId('devices-list'); list.replaceChildren();
    if (!devices.length) { list.append(node('p', '暂无登录设备。', 'muted')); return; }
    for (const device of devices) {
      const row = node('div', undefined, 'device-row');
      const content = node('div', undefined, 'row-content');
      content.append(node('div', device.deviceName, 'row-title'));
      if (device.current) content.append(node('span', '当前设备', 'tag'));
      content.append(node('div', `最近活动：${date(device.lastSeen)}`, 'row-meta'));
      const revoke = button('撤销登录', () => confirmAction('撤销设备登录', `撤销「${device.deviceName}」的登录？${device.current ? '当前页面将退出登录。' : '该设备需要重新登录。'}`, async () => {
        await api(`/devices/${encodeURIComponent(device.sessionId)}`, 'DELETE');
        if (device.current || device.sessionId === sessionId) resetSession('已撤销当前设备登录。');
        else { await refresh(); notice('已撤销设备登录。'); }
      }));
      row.append(content, revoke); list.append(row);
    }
  }
  function renderUsers(users) {
    const list = byId('users-list'); list.replaceChildren();
    byId('users-count').textContent = `${users.length} 个用户`;
    for (const user of users) {
      const own = user.accountId === account.accountId;
      const row = node('div', undefined, 'user-row');
      const content = node('div', undefined, 'row-content');
      content.append(node('div', user.username, 'row-title'));
      content.append(node('div', `${user.role === 'admin' ? '管理员' : '普通用户'} · ${user.disabled ? '已禁用' : '正常'}${own ? ' · 本人' : ''} · ${user.sessionCount} 个登录`, 'row-meta'));
      content.append(node('div', `注册：${date(user.createdAt)} · ${user.accountId}`, 'row-meta'));
      const actions = node('div', undefined, 'row-actions');
      const status = button(user.disabled ? '启用' : '禁用', () => confirmAction(user.disabled ? '启用用户' : '禁用用户', `${user.disabled ? '启用' : '禁用'}「${user.username}」？${user.disabled ? '启用后可以重新登录。' : '该用户的所有登录将立即失效。'}`, async () => {
        await api(`/admin/users/${encodeURIComponent(user.accountId)}/status`, 'POST', { disabled: !user.disabled });
        await refresh(); notice(user.disabled ? '已启用用户。' : '已禁用用户。');
      }), own);
      status.dataset.action = 'status';
      const password = button('重置密码', () => confirmAction('重置用户密码', `为「${user.username}」设置新密码？该用户的所有登录将立即失效。`, async newPassword => {
        await api(`/admin/users/${encodeURIComponent(user.accountId)}/password`, 'POST', { newPassword });
        await refresh(); notice('已重置密码，请将新密码告知该用户。');
      }, true), own);
      password.dataset.action = 'password';
      const revoke = button('撤销所有登录', () => confirmAction('撤销用户所有登录', `撤销「${user.username}」的所有登录？${own ? '当前页面也将退出登录。' : '该用户需要重新登录。'}`, async () => {
        await api(`/admin/users/${encodeURIComponent(user.accountId)}/revoke`, 'POST', {});
        if (own) resetSession('已撤销所有登录。');
        else { await refresh(); notice('已撤销用户所有登录。'); }
      }));
      revoke.dataset.action = 'revoke';
      actions.append(status, password, revoke); row.append(content, actions); list.append(row);
    }
  }
  async function refresh() {
    const active = token;
    const me = await api('/me');
    if (token !== active) throw silent();
    const devices = await api('/devices');
    if (token !== active) throw silent();
    account = me;
    byId('account-username').textContent = me.username;
    byId('account-id').textContent = me.accountId;
    byId('account-role').textContent = me.role === 'admin' ? '管理员' : '普通用户';
    renderDevices(devices.devices);
    byId('admin-view').hidden = me.role !== 'admin';
    if (me.role === 'admin') {
      const users = await api('/admin/users');
      const invite = await api('/admin/invite');
      if (token !== active) throw silent();
      renderUsers(users.users);
      byId('current-invite').textContent = invite.inviteCode;
    }
  }
  function matching(first, second) {
    if (first.value === second.value) return true;
    second.setCustomValidity('两次输入的密码不一致。'); second.reportValidity(); return false;
  }
  for (const [first, second] of [['register-password', 'register-password-confirm'], ['new-password', 'new-password-confirm'], ['confirm-password', 'confirm-password-repeat']]) {
    for (const id of [first, second]) byId(id).addEventListener('input', () => byId(second).setCustomValidity(''));
  }
  byId('login-tab').addEventListener('click', () => { showAuth('login'); notice(); });
  byId('register-tab').addEventListener('click', () => { showAuth('register'); notice(); });
  for (const mode of ['login', 'register']) {
    byId(`${mode}-form`).addEventListener('submit', event => {
      event.preventDefault();
      if (mode === 'register' && !matching(byId('register-password'), byId('register-password-confirm'))) return;
      const body = { username: byId(`${mode}-username`).value.trim(), password: byId(`${mode}-password`).value, deviceName: '网页管理', deviceId };
      if (mode === 'register') body.inviteCode = byId('register-invite').value.trim();
      run(event.submitter, async () => {
        sessionGeneration++;
        const session = await api(`/auth/${mode}`, 'POST', body);
        token = session.token; sessionId = session.sessionId;
        byId('auth-view').hidden = true;
        byId('account-view').hidden = false;
        byId('session-actions').hidden = false;
        for (const form of [byId('login-form'), byId('register-form')]) form.reset();
        await refresh(); notice('已登录。');
      });
    });
  }
  byId('refresh-button').addEventListener('click', event => run(event.currentTarget, async () => { await refresh(); notice('已刷新。'); }));
  byId('logout-button').addEventListener('click', event => run(event.currentTarget, async () => {
    try { await api('/auth/logout', 'POST', {}); }
    catch (error) {
      if (error.silent) return;
      resetSession('已退出此页面。服务器暂时无法撤销登录，可稍后在设备列表中撤销。'); return;
    }
    resetSession('已退出登录。');
  }));
  byId('password-form').addEventListener('submit', event => {
    event.preventDefault();
    if (!matching(byId('new-password'), byId('new-password-confirm'))) return;
    const body = { oldPassword: byId('old-password').value, newPassword: byId('new-password').value };
    run(event.submitter, async () => { await api('/auth/password', 'POST', body); byId('password-form').reset(); await refresh(); notice('密码已修改，其他设备已退出登录。'); });
  });
  byId('invite-form').addEventListener('submit', event => {
    event.preventDefault();
    const inviteCode = byId('invite-code').value;
    confirmAction('更新注册邀请码', '确认更新邀请码？原邀请码将无法用于注册。', async () => {
      await api('/admin/invite', 'POST', { inviteCode });
      await refresh(); byId('invite-form').reset(); notice('邀请码已更新。');
    });
  });
  byId('confirm-cancel').addEventListener('click', () => { if (!confirming) byId('confirm-dialog').close(); });
  byId('confirm-dialog').addEventListener('cancel', event => { if (confirming) event.preventDefault(); });
  byId('confirm-dialog').addEventListener('close', () => { pendingConfirmation = null; byId('confirm-form').reset(); });
  byId('confirm-form').addEventListener('submit', async event => {
    event.preventDefault();
    if (confirming || !pendingConfirmation) return;
    if (!byId('confirm-password-fields').hidden && !matching(byId('confirm-password'), byId('confirm-password-repeat'))) return;
    confirming = true; byId('confirm-submit').disabled = true; byId('confirm-cancel').disabled = true;
    byId('confirm-error').hidden = true;
    const action = pendingConfirmation;
    const password = byId('confirm-password').value;
    try { await action(password); byId('confirm-dialog').close(); }
    catch (error) { if (!error.silent) { byId('confirm-error').textContent = error.message; byId('confirm-error').hidden = false; } }
    finally { confirming = false; byId('confirm-submit').disabled = false; byId('confirm-cancel').disabled = false; }
  });
})();
