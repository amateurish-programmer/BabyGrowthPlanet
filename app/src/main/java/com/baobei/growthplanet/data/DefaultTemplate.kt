package com.baobei.growthplanet.data

object DefaultTemplate {
    data class Category(val title: String, val emoji: String, val items: List<String>)

    val categories = listOf(
        Category("生活习惯", "🌱", listOf("早晚刷牙洗脸", "自己穿衣叠被", "按时吃饭不挑食", "自己整理玩具", "按时上床睡觉")),
        Category("情绪管理", "🌈", listOf("用说话表达感受", "遇到难过会求助", "遇困难不放弃")),
        Category("手足相处", "🤝", listOf("和弟弟妹妹友好相处", "主动分享或照顾家人")),
        Category("社交能力", "🎈", listOf("主动和小朋友一起玩", "遵守规则懂礼貌")),
        Category("学习态度", "📚", listOf("上课认真听讲", "按时完成作业", "复习当天学习内容")),
        Category("时间管理", "⏰", listOf("做事不拖延磨蹭", "说到做到守约定")),
    )

    val deductions = listOf(
        "和别人发生肢体冲突" to 2,
        "无合理理由哭一次" to 2,
        "情绪失控需要长时间安抚" to 2,
        "提醒多次仍严重拖延" to 1,
        "用伤人的话或动作对待他人" to 2,
    )

    val rewards = listOf(
        Triple("一起看动画片 30 分钟", "📺", 20),
        Triple("选一份喜欢的小零食", "🍪", 50),
        Triple("亲子游戏时间", "🎲", 100),
        Triple("去游乐场玩一次", "🎡", 150),
        Triple("全家体验日", "🏕️", 200),
    )

    val guidance = listOf(
        "每天睡前和孩子一起回顾，先听孩子怎么说。",
        "重点看今天做到了什么，而不是只盯着没做到的事。",
        "奖励尽量包含陪伴、体验和共同阅读，不只奖励物品。",
        "每周只选 1–3 个小目标，让进步更容易被看见。",
        "标准可以循序渐进，稳定参与比天天满分更重要。",
    )
}
