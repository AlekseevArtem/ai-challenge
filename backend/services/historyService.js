/**
 * Сервис для управления историей диалога
 * Сохраняет сообщения пользователя и полные ответы Claude API
 */

const fs = require('fs').promises;
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const { calculateCost } = require('../utils/pricing');

const STORAGE_PATH = path.join(__dirname, '../storage');
const HISTORY_FILE = path.join(STORAGE_PATH, 'history.json');

class HistoryService {
    constructor() {
        this.history = [];
        this.initialized = false;
    }

    /**
     * Инициализация сервиса - загрузка истории из файла
     */
    async initialize() {
        if (this.initialized) return;

        try {
            // Создаем директорию storage если её нет
            await fs.mkdir(STORAGE_PATH, { recursive: true });

            // Пытаемся загрузить историю
            try {
                const data = await fs.readFile(HISTORY_FILE, 'utf-8');
                this.history = JSON.parse(data);
                console.log(`📚 История загружена: ${this.history.length} сообщений`);
            } catch (err) {
                if (err.code === 'ENOENT') {
                    console.log('📚 История не найдена, создаем новую');
                    this.history = [];
                    await this.save();
                } else {
                    throw err;
                }
            }

            this.initialized = true;
        } catch (error) {
            console.error('Ошибка инициализации HistoryService:', error);
            throw error;
        }
    }

    /**
     * Сохранение истории в файл
     */
    async save() {
        try {
            await fs.writeFile(
                HISTORY_FILE,
                JSON.stringify(this.history, null, 2),
                'utf-8'
            );
        } catch (error) {
            console.error('Ошибка сохранения истории:', error);
            throw error;
        }
    }

    /**
     * Добавление нового сообщения в историю
     * @param {string} userMessage - сообщение пользователя
     * @param {object} apiResponse - полный ответ от Claude API
     * @returns {object} созданное сообщение с метаданными
     */
    async addMessage(userMessage, apiResponse) {
        await this.initialize();

        const message = {
            id: uuidv4(),
            timestamp: new Date().toISOString(),
            type: 'message', // 'message' или 'summary'
            user: userMessage,
            bot: apiResponse.content?.[0]?.text || '',
            api: {
                id: apiResponse.id,
                model: apiResponse.model,
                usage: apiResponse.usage,
                stopReason: apiResponse.stop_reason,
                stopSequence: apiResponse.stop_sequence
            },
            cost: calculateCost(
                apiResponse.model,
                apiResponse.usage?.input_tokens || 0,
                apiResponse.usage?.output_tokens || 0
            )
        };

        this.history.push(message);
        await this.save();

        console.log(`💬 Сообщение сохранено: ${message.id}`);
        return message;
    }

    /**
     * Добавление summary объекта в историю
     * @param {array} summarizedMessages - ID сообщений, которые были сжаты
     * @param {string} summaryText - текст summary
     * @param {object} apiResponse - ответ API при создании summary
     */
    async addSummary(summarizedMessages, summaryText, apiResponse) {
        await this.initialize();

        const summary = {
            id: uuidv4(),
            timestamp: new Date().toISOString(),
            type: 'summary',
            summarizedMessages, // ID сообщений, которые были сжаты
            summary: summaryText,
            api: {
                id: apiResponse.id,
                model: apiResponse.model,
                usage: apiResponse.usage
            },
            cost: calculateCost(
                apiResponse.model,
                apiResponse.usage?.input_tokens || 0,
                apiResponse.usage?.output_tokens || 0
            )
        };

        // Удаляем сжатые сообщения и добавляем summary
        this.history = this.history.filter(
            msg => !summarizedMessages.includes(msg.id)
        );
        this.history.push(summary);

        await this.save();

        console.log(`📦 Summary создан, сжато ${summarizedMessages.length} сообщений`);
        return summary;
    }

    /**
     * Получение всей истории
     */
    async getHistory() {
        await this.initialize();
        return this.history;
    }

    /**
     * Получение последних N сообщений
     */
    async getRecentMessages(count = 10) {
        await this.initialize();
        return this.history.slice(-count);
    }

    /**
     * Получение сообщения по ID
     */
    async getMessageById(id) {
        await this.initialize();
        return this.history.find(msg => msg.id === id);
    }

    /**
     * Получение сообщений для отправки в API
     * Конвертирует историю в формат Claude API
     */
    async getMessagesForApi() {
        await this.initialize();

        const messages = [];

        for (const item of this.history) {
            if (item.type === 'summary') {
                // Добавляем summary как системное сообщение от assistant
                messages.push({
                    role: 'assistant',
                    content: `[Краткое содержание предыдущих сообщений: ${item.summary}]`
                });
            } else if (item.type === 'message') {
                // Добавляем обычные сообщения
                messages.push({
                    role: 'user',
                    content: item.user
                });
                messages.push({
                    role: 'assistant',
                    content: item.bot
                });
            }
        }

        return messages;
    }

    /**
     * Подсчёт количества обычных сообщений (не summary)
     */
    async getRegularMessagesCount() {
        await this.initialize();
        return this.history.filter(msg => msg.type === 'message').length;
    }

    /**
     * Очистка истории (для тестирования)
     */
    async clear() {
        this.history = [];
        await this.save();
        console.log('🗑️ История очищена');
    }
}

// Создаем единственный экземпляр сервиса (Singleton)
const historyService = new HistoryService();

module.exports = historyService;