/**
 * Сервис для автоматического сжатия истории диалога
 * Каждые N сообщений создаёт краткое содержание
 */

const axios = require('axios');

const SUMMARIZATION_CONFIG = {
    THRESHOLD: 5,
    MODEL: 'claude-sonnet-4-5-20250929',
    MAX_TOKENS: 300
};

class SummarizationService {
    constructor(apiKey, apiUrl, anthropicVersion) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.anthropicVersion = anthropicVersion;
    }

    /**
     * Проверка, нужно ли сжимать историю
     * @param {number} regularMessagesCount - количество обычных сообщений
     */
    shouldSummarize(regularMessagesCount) {
        return regularMessagesCount >= SUMMARIZATION_CONFIG.THRESHOLD;
    }

    /**
     * Создание summary для набора сообщений
     * @param {array} messages - массив сообщений для сжатия
     * @returns {object} - { summary: string, apiResponse: object }
     */
    async createSummary(messages) {
        // Формируем промпт для создания summary
        const conversationText = messages
            .map(msg => {
                if (msg.type === 'message') {
                    return `Пользователь: ${msg.user}\nБот: ${msg.bot}`;
                }
                return '';
            })
            .join('\n\n');

        const summaryPrompt = `Пожалуйста, создай краткое содержание следующего диалога. Включи ключевые факты, важный контекст и основные темы обсуждения. Это summary будет использоваться для продолжения диалога.

${conversationText}

Краткое содержание (на русском языке):`;

        try {
            const response = await axios.post(
                this.apiUrl,
                {
                    model: SUMMARIZATION_CONFIG.MODEL,
                    messages: [
                        {
                            role: 'user',
                            content: summaryPrompt
                        }
                    ],
                    max_tokens: SUMMARIZATION_CONFIG.MAX_TOKENS
                },
                {
                    headers: {
                        'X-API-Key': this.apiKey,
                        'Content-Type': 'application/json',
                        'Anthropic-Version': this.anthropicVersion
                    }
                }
            );

            const summaryText = response.data.content?.[0]?.text;

            if (!summaryText) {
                throw new Error('Не удалось создать summary');
            }

            console.log(`📝 Summary создан (${response.data.usage?.output_tokens} токенов)`);

            return {
                summary: summaryText,
                apiResponse: response.data
            };
        } catch (error) {
            console.error('Ошибка создания summary:', error.response?.data || error.message);
            throw error;
        }
    }

    /**
     * Автоматическое сжатие истории при необходимости
     * @param {object} historyService - экземпляр HistoryService
     */
    async autoSummarize(historyService) {
        const regularMessagesCount = await historyService.getRegularMessagesCount();

        console.log(`📊 Проверка сжатия: ${regularMessagesCount} обычных сообщений`);

        if (!this.shouldSummarize(regularMessagesCount)) {
            console.log(`⏳ Сжатие не требуется (порог: ${SUMMARIZATION_CONFIG.THRESHOLD})`);
            return null;
        }

        console.log(`🔄 Начинаем сжатие истории (${regularMessagesCount} сообщений)`);

        // Получаем все сообщения
        const allHistory = await historyService.getHistory();

        // Находим первые N обычных сообщений
        const messagesToSummarize = [];
        for (const item of allHistory) {
            if (item.type === 'message' && messagesToSummarize.length < SUMMARIZATION_CONFIG.THRESHOLD) {
                messagesToSummarize.push(item);
            }
        }

        if (messagesToSummarize.length === 0) {
            console.log('⚠️ Нет сообщений для сжатия');
            return null;
        }

        console.log(`📝 Сжимаем ${messagesToSummarize.length} сообщений...`);

        // Создаём summary
        const { summary, apiResponse } = await this.createSummary(messagesToSummarize);

        // Сохраняем summary и удаляем старые сообщения
        const summarizedMessageIds = messagesToSummarize.map(msg => msg.id);
        const summaryObject = await historyService.addSummary(
            summarizedMessageIds,
            summary,
            apiResponse
        );

        console.log(`✅ История сжата: ${summarizedMessageIds.length} → 1 summary`);

        return summaryObject;
    }
}

module.exports = SummarizationService;