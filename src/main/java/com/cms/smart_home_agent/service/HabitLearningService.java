package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.entity.HabitDataLog;
import com.cms.smart_home_agent.mapper.HabitDataLogMapper;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class HabitLearningService {

    @Autowired
    private HabitDataLogMapper habitMapper;

    /**
     * @param userId     当前操作的用户ID
     * @param familyId   当前操作的家庭ID
     * @param currentOut 当前实时室外温
     * @param currentIn  当前实时室内温
     */
    public double getPersonalizedTemp(Integer userId, Integer familyId, double currentOut, double currentIn) {

        // 1. 获取该用户相关的历史数据（建议取 50 条，特征多了需要更多样本支撑）
        List<HabitDataLog> logs = habitMapper.findRecentLogsBySpecificContext(userId,familyId,50);

        // 2. 健壮性检查：4个变量+1个截距，至少需要 5 个以上样本
        //要是一个老用户在新家庭里，就调用他在别的地方的家的记录来训练
        if (logs.size() < 8) {
            logs = habitMapper.findRecentLogsByUserId(userId, 10);
        }
        if (logs.size() < 8) {
            return 26.0; // 数据不足时返回一个合理的默认值
        }

        // 3. 准备标签 Y
        double[] targetY = logs.stream().mapToDouble(HabitDataLog::getTargetTemp).toArray();

        // 4. 准备特征矩阵 X (维度：N 行 x 4 列)
        double[][] featuresX = new double[logs.size()][4];
        for (int i = 0; i < logs.size(); i++) {
            HabitDataLog log = logs.get(i);
            featuresX[i][0] = log.getOutdoorTemp();
            featuresX[i][1] = log.getIndoorTemp();
            featuresX[i][2] = log.getUserId().doubleValue();   // 新增：用户 ID 参数
            featuresX[i][3] = log.getFamilyId().doubleValue(); // 新增：家庭 ID 参数
        }

        // 5. 模型拟合
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        try {
            regression.newSampleData(targetY, featuresX);

            // 6. 获取回归系数 [b0(截距), w1(室外), w2(室内), w3(用户ID), w4(家庭ID)]
            double[] beta = regression.estimateRegressionParameters();

            // 7. 执行预测
            double prediction = beta[0]
                    + beta[1] * currentOut
                    + beta[2] * currentIn
                    + beta[3] * userId.doubleValue()
                    + beta[4] * familyId.doubleValue();

            // 8. 结果格式化：限制在 [16, 30] 范围内并保留一位小数
            return Math.round(Math.max(16.0, Math.min(30.0, prediction)) * 10.0) / 10.0;

        } catch (Exception e) {
            // 如果数据线性相关性太强导致矩阵求逆失败，返回默认值
            return 26.0;
        }
    }
}