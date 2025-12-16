const API_URL = 'http://localhost:3000';

let tools = [];
let selectedTool = null;

async function fetchTools() {
    const statusEl = document.getElementById('status');
    const toolsListEl = document.getElementById('tools-list');

    try {
        statusEl.textContent = '⏳ Подключение...';
        statusEl.className = 'status loading';

        const response = await fetch(`${API_URL}/api/mcp/tools`);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();

        if (data.success && data.tools) {
            tools = data.tools;
            displayTools(tools);
            statusEl.textContent = `✅ Подключено (${tools.length} инструментов)`;
            statusEl.className = 'status connected';
        } else {
            throw new Error('Неверный формат ответа от сервера');
        }
    } catch (error) {
        console.error('Ошибка загрузки инструментов:', error);
        statusEl.textContent = '❌ Ошибка подключения';
        statusEl.className = 'status disconnected';

        toolsListEl.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">⚠️</div>
                <p>Ошибка загрузки инструментов</p>
                <p style="font-size: 0.9em; margin-top: 10px;">${error.message}</p>
                <button class="btn" style="margin-top: 20px;" onclick="fetchTools()">Повторить</button>
            </div>
        `;
    }
}

function displayTools(tools) {
    const toolsListEl = document.getElementById('tools-list');

    if (!tools || tools.length === 0) {
        toolsListEl.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📭</div>
                <p>Инструменты не найдены</p>
            </div>
        `;
        return;
    }

    toolsListEl.innerHTML = tools.map(tool => `
        <div class="tool-item" onclick="selectTool('${tool.name}')">
            <div class="tool-name">${tool.name}</div>
            <div class="tool-description">${tool.description}</div>
            <div class="tool-schema">${JSON.stringify(tool.inputSchema, null, 2)}</div>
        </div>
    `).join('');
}

function selectTool(toolName) {
    selectedTool = tools.find(t => t.name === toolName);

    if (!selectedTool) return;

    document.querySelectorAll('.tool-item').forEach(item => {
        item.classList.remove('selected', 'expanded');
    });

    const selectedItem = Array.from(document.querySelectorAll('.tool-item'))
        .find(item => item.querySelector('.tool-name').textContent === toolName);

    if (selectedItem) {
        selectedItem.classList.add('selected', 'expanded');
    }

    displayCallPanel(selectedTool);
}

function displayCallPanel(tool) {
    const callPanelEl = document.getElementById('call-panel');
    const schema = tool.inputSchema;
    const properties = schema.properties || {};
    const required = schema.required || [];

    let inputsHTML = '';

    if (Object.keys(properties).length === 0) {
        inputsHTML = '<p style="color: #9ca3af; text-align: center;">Этот инструмент не требует параметров</p>';
    } else {
        inputsHTML = Object.entries(properties).map(([key, prop]) => {
            const isRequired = required.includes(key);
            const label = `${key}${isRequired ? ' *' : ''}`;

            if (prop.enum) {
                const options = prop.enum.map(val =>
                    `<option value="${val}">${val}</option>`
                ).join('');

                return `
                    <div class="input-group">
                        <label for="input-${key}">${label}</label>
                        <select id="input-${key}" ${isRequired ? 'required' : ''}>
                            <option value="">-- Выберите --</option>
                            ${options}
                        </select>
                        ${prop.description ? `<small style="color: #6b7280;">${prop.description}</small>` : ''}
                    </div>
                `;
            } else if (prop.type === 'number') {
                return `
                    <div class="input-group">
                        <label for="input-${key}">${label}</label>
                        <input type="number" id="input-${key}" placeholder="${prop.description || ''}" ${isRequired ? 'required' : ''}>
                        ${prop.description ? `<small style="color: #6b7280;">${prop.description}</small>` : ''}
                    </div>
                `;
            } else {
                return `
                    <div class="input-group">
                        <label for="input-${key}">${label}</label>
                        <input type="text" id="input-${key}" placeholder="${prop.description || ''}" ${isRequired ? 'required' : ''}>
                        ${prop.description ? `<small style="color: #6b7280;">${prop.description}</small>` : ''}
                    </div>
                `;
            }
        }).join('');
    }

    callPanelEl.innerHTML = `
        <div class="input-group">
            <label>Инструмент</label>
            <input type="text" value="${tool.name}" disabled style="background: #f3f4f6;">
        </div>

        ${inputsHTML}

        <button class="btn" onclick="callTool()">▶️ Выполнить</button>

        <div id="result-container"></div>
    `;
}

async function callTool() {
    if (!selectedTool) return;

    const resultContainer = document.getElementById('result-container');
    const schema = selectedTool.inputSchema;
    const properties = schema.properties || {};

    const args = {};
    for (const key in properties) {
        const inputEl = document.getElementById(`input-${key}`);
        if (inputEl) {
            let value = inputEl.value;

            if (properties[key].type === 'number') {
                value = parseFloat(value);
                if (isNaN(value)) {
                    showError(resultContainer, `Параметр "${key}" должен быть числом`);
                    return;
                }
            }

            if (value !== '') {
                args[key] = value;
            }
        }
    }

    const required = schema.required || [];
    for (const key of required) {
        if (!(key in args) || args[key] === '') {
            showError(resultContainer, `Обязательный параметр "${key}" не заполнен`);
            return;
        }
    }

    resultContainer.innerHTML = `
        <div class="loading show">
            <div class="spinner"></div>
            <p>Вызов инструмента...</p>
        </div>
    `;

    try {
        const response = await fetch(`${API_URL}/api/mcp/call`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                name: selectedTool.name,
                arguments: args,
            }),
        });

        const data = await response.json();

        if (!response.ok || !data.success) {
            throw new Error(data.error || 'Ошибка вызова инструмента');
        }

        showResult(resultContainer, data);
    } catch (error) {
        console.error('Ошибка вызова инструмента:', error);
        showError(resultContainer, error.message);
    }
}

function showResult(container, data) {
    let resultText = '';

    if (data.result && data.result.content) {
        resultText = data.result.content
            .map(item => item.text || JSON.stringify(item, null, 2))
            .join('\n\n');
    } else {
        resultText = JSON.stringify(data, null, 2);
    }

    container.innerHTML = `
        <div class="result-box">
            <h3>✅ Результат</h3>
            <div class="result-content">${escapeHtml(resultText)}</div>
        </div>
    `;
}

function showError(container, message) {
    container.innerHTML = `
        <div class="result-box error">
            <h3>❌ Ошибка</h3>
            <div class="result-content">${escapeHtml(message)}</div>
        </div>
    `;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

window.addEventListener('DOMContentLoaded', () => {
    fetchTools();
});
