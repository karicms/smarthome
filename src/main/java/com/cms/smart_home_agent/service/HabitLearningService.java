package com.cms.smart_home_agent.service;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HabitLearningService {

    /**
     * @param outdoorHistory 历史室外温度列表
     * @param indoorHistory  历史室内温度列表
     * @param targetHistory  历史用户设定的目标温度列表
     * @param currentOut     当前实时室外温
     * @param currentIn      当前实时室内温
     * @return AI 预测的建议温度
     */
    public double calculateRecommendedTemp(List<Double> outdoorHistory,
                                           List<Double> indoorHistory,
                                           List<Double> targetHistory,
                                           double currentOut,
                                           double currentIn) {

        // 健壮性检查：线性回归至少需要比变量数更多的数据点
        if (outdoorHistory.size() < 5) {
            return 26.0; // 数据不足时返回安全默认值
        }

        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();

        // 1. 整理标签 Y (用户设定的温度)
        double[] y = targetHistory.stream().mapToDouble(Double::doubleValue).toArray();

        // 2. 整理特征 X (室外温, 室内温)
        double[][] x = new double[outdoorHistory.size()][2];
        for (int i = 0; i < outdoorHistory.size(); i++) {
            x[i][0] = outdoorHistory.get(i);
            x[i][1] = indoorHistory.get(i);
        }

        // 3. 拟合模型
        regression.newSampleData(y, x);

        // 4. 获取回归系数 [b (截距), w1 (室外权重), w2 (室内权重)]
        double[] beta = regression.estimateRegressionParameters();

        // 5. 预测：Target = b + w1 * CurrentOut + w2 * CurrentIn
        double prediction = beta[0] + beta[1] * currentOut + beta[2] * currentIn;

        // 6. 结果约束（防止模型跑飞，限制在空调正常范围内）
        return Math.round(Math.max(16, Math.min(30, prediction)) * 10.0) / 10.0;
    }
}