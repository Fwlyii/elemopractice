/**
 * 后端AI代理服务示例 (Node.js + Express)
 * 用于安全地调用阿里云通义千问API
 * 
 * 安装依赖：
 * npm install express axios cors dotenv
 * 
 * 环境变量配置（.env文件）：
 * ALIYUN_API_KEY=your-dashscope-api-key
 * PORT=3001
 */

const express = require('express')
const axios = require('axios')
const cors = require('cors')
require('dotenv').config()

const app = express()
const PORT = process.env.PORT || 3001

// 中间件
app.use(cors())
app.use(express.json())

// 阿里云通义千问API配置
const ALIYUN_CONFIG = {
  endpoint: 'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation',
  apiKey: process.env.ALIYUN_API_KEY,
  model: 'qwen-turbo' // 可选：qwen-plus, qwen-max
}

// AI聊天接口
app.post('/api/ai/chat', async (req, res) => {
  try {
    const { message, history = [] } = req.body

    if (!message || typeof message !== 'string') {
      return res.status(400).json({
        success: false,
        message: '消息内容不能为空'
      })
    }

    // 构建对话历史
    const messages = [
      {
        role: 'system',
        content: `你是饿了么平台的AI助手，专门帮助用户解决外卖订餐相关的问题。你应该：
1. 友好、专业地回答用户问题
2. 重点关注美食推荐、订单查询、优惠活动、配送服务等话题
3. 如果用户询问与外卖无关的内容，礼貌地引导回到平台相关话题
4. 回答要简洁明了，适合移动端显示，使用emoji让回答更生动
5. 当用户询问具体商家信息时，可以推荐虾滑火锅、螺狮粉、黄焖鸡米饭等热门商家`
      }
    ]

    // 添加历史对话（最近10条）
    if (history.length > 0) {
      const recentHistory = history.slice(-10)
      recentHistory.forEach(msg => {
        if (msg.type === 'user') {
          messages.push({ role: 'user', content: msg.content })
        } else if (msg.type === 'ai') {
          messages.push({ role: 'assistant', content: msg.content })
        }
      })
    }

    // 添加当前用户消息
    messages.push({ role: 'user', content: message })

    // 调用阿里云API
    const requestBody = {
      model: ALIYUN_CONFIG.model,
      input: { messages },
      parameters: {
        max_tokens: 800,
        temperature: 0.8,
        top_p: 0.9
      }
    }

    console.log('调用阿里云API:', {
      url: ALIYUN_CONFIG.endpoint,
      model: ALIYUN_CONFIG.model,
      messageCount: messages.length
    })

    const response = await axios.post(ALIYUN_CONFIG.endpoint, requestBody, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${ALIYUN_CONFIG.apiKey}`,
        'X-DashScope-SSE': 'disable'
      },
      timeout: 30000 // 30秒超时
    })

    if (response.data && response.data.output && response.data.output.choices && response.data.output.choices[0]) {
      const aiReply = response.data.output.choices[0].message.content

      res.json({
        success: true,
        data: {
          reply: aiReply,
          usage: response.data.usage || null
        }
      })

      console.log('AI回复成功:', {
        userMessage: message.substring(0, 50) + '...',
        replyLength: aiReply.length,
        usage: response.data.usage
      })
    } else {
      throw new Error('API响应格式不正确')
    }

  } catch (error) {
    console.error('AI API调用失败:', error.message)
    
    // 根据错误类型返回不同的备用回复
    let fallbackReply = '抱歉，我暂时无法回答您的问题。请稍后再试，或者联系客服获取帮助。'
    
    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      fallbackReply = '网络连接超时，请稍后重试。如果问题持续，请联系客服。'
    } else if (error.response?.status === 429) {
      fallbackReply = '当前访问量较大，请稍后再试。感谢您的耐心等待！'
    } else if (error.response?.status === 401) {
      fallbackReply = '服务暂时不可用，我们正在修复中。请稍后再试。'
    }

    res.json({
      success: true,
      data: {
        reply: fallbackReply
      }
    })
  }
})

// 健康检查接口
app.get('/api/health', (req, res) => {
  res.json({
    success: true,
    message: 'AI代理服务运行正常',
    timestamp: new Date().toISOString()
  })
})

// 获取AI服务状态
app.get('/api/ai/status', (req, res) => {
  res.json({
    success: true,
    data: {
      model: ALIYUN_CONFIG.model,
      available: !!ALIYUN_CONFIG.apiKey,
      timestamp: new Date().toISOString()
    }
  })
})

// 错误处理中间件
app.use((error, req, res, next) => {
  console.error('服务器错误:', error)
  res.status(500).json({
    success: false,
    message: '服务器内部错误'
  })
})

// 404处理
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: '接口不存在'
  })
})

// 启动服务器
app.listen(PORT, () => {
  console.log(`🚀 AI代理服务已启动`)
  console.log(`🌐 服务地址: http://localhost:${PORT}`)
  console.log(`🔑 API Key状态: ${ALIYUN_CONFIG.apiKey ? '已配置' : '未配置'}`)
  console.log(`🤖 AI模型: ${ALIYUN_CONFIG.model}`)
})

// 优雅关闭
process.on('SIGTERM', () => {
  console.log('📴 收到SIGTERM信号，正在关闭服务器...')
  process.exit(0)
})

process.on('SIGINT', () => {
  console.log('📴 收到SIGINT信号，正在关闭服务器...')
  process.exit(0)
})

module.exports = app
