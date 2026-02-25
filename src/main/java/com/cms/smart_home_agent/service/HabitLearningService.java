package com.cms.smart_home_agent.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HabitLearningService {

    // 线性回归：Y = w1*Outdoor + w2*Indoor + b
    // 为了毕设好展示，我们用两个特征：室外温和室内温
    public double predict(List<Double> outdoors, List<Double> indoors, List<Double> targets,
                          double currentOut, double currentIn) {

        // 如果数据太少（比如少于3条），直接返回默认舒适温度26，避免计算报错
        if (outdoors.size() < 3) return 26.0;

        // 演示用：简单的多元线性回归（基于最小二乘法原理）
        // 这里推荐引入 Apache Commons Math 库，代码会非常整洁
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();

        // 转换数据格式为矩阵
        double[] y = targets.stream().mapToDouble(Double::doubleValue).toArray();
        double[][] x = new double[outdoors.size()][2];
        for (int i = 0; i < outdoors.size(); i++) {
            x[i][0] = outdoors.get(i);
            x[i][1] = indoors.get(i);
        }

        regression.newSampleData(y, x);
        double[] beta = regression.estimateRegressionParameters(); // 得到 [b, w1, w2]

        // 预测结果 = b + w1*currentOut + w2*currentIn
        double result = beta[0] + beta[1] * currentOut + beta[2] * currentIn;

        // 限制在合理范围内（16-30度）
        return Math.max(16, Math.min(30, result));
    }
}