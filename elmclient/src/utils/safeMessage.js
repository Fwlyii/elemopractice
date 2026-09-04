/**
 * 将用户或模型返回的纯文本转换为极小的安全富文本子集。
 * 必须先转义，再添加我们自己生成的标签；不要直接把模型输出交给 v-html。
 */
export function formatSafeMessage(content) {
  if (!content || typeof content !== 'string') return '消息内容为空'

  const escaped = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

  return escaped
    .replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\r?\n/g, '<br>')
}
