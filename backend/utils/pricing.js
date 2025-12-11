/**
 * Утилита для расчёта стоимости использования Claude API
 */

// Цены за 1M токенов (в долларах) для разных моделей
const PRICING = {
    'claude-sonnet-4-5-20250929': {
        input: 3.00,    // $3 за 1M input токенов
        output: 15.00   // $15 за 1M output токенов
    },
    'claude-opus-4-5-20251101': {
        input: 15.00,
        output: 75.00
    },
    'claude-3-5-sonnet-20241022': {
        input: 3.00,
        output: 15.00
    }
};

/**
 * Вычисляет стоимость использования модели
 * @param {string} model - название модели
 * @param {number} inputTokens - количество входных токенов
 * @param {number} outputTokens - количество выходных токенов
 * @returns {number} стоимость в долларах
 */
function calculateCost(model, inputTokens, outputTokens) {
    const pricing = PRICING[model];

    if (!pricing) {
        console.warn(`Цены для модели ${model} не найдены, используются дефолтные значения`);
        return 0;
    }

    const inputCost = (inputTokens / 1_000_000) * pricing.input;
    const outputCost = (outputTokens / 1_000_000) * pricing.output;

    return inputCost + outputCost;
}

/**
 * Форматирует стоимость для отображения
 * @param {number} cost - стоимость в долларах
 * @returns {string} форматированная строка
 */
function formatCost(cost) {
    if (cost < 0.01) {
        return `$${(cost * 1000).toFixed(4)}k`; // в милли-долларах
    }
    return `$${cost.toFixed(4)}`;
}

module.exports = {
    calculateCost,
    formatCost,
    PRICING
};