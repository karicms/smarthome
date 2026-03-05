package com.cms.smart_home_agent.HbaitLearn;

import com.cms.smart_home_agent.service.HabitLearningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;

@SpringBootTest
public class HabitLearningTest {
    @Autowired
    private HabitLearningService habitLearningService;

    @Test
    void testRecommendation()
    {
        double temp1 = habitLearningService.getPersonalizedTemp(1,1,33.0,29.0);
        System.out.println("user1 in family1 personality temp is:"+temp1+"℃");

        double temp2 = habitLearningService.getPersonalizedTemp(1,2,33.0,29.0);
        System.out.println("用户1在家庭2的建议温度: " + temp2 + "℃");
    }
}
