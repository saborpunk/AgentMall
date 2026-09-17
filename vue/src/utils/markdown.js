import { marked } from 'marked'

// 大模型返回的内容常带 Markdown 语法（标题、列表、粗体、表格），
// 直接放在页面上会显示成一堆 # 和 *，所以统一在这里转成 HTML 再渲染。
marked.setOptions({
  // 把单个换行也当成 <br>，模型输出的分行才不会被合并成一段
  breaks: true,
  // 按 GitHub 风格解析，支持表格和删除线
  gfm: true
})

/**
 * 把 Markdown 文本转成 HTML。
 * 内容为空时返回一段占位提示，避免页面上出现空白区域。
 *
 * @param text 大模型返回的原始文本
 * @param emptyText 内容为空时显示的文字
 */
export function renderMarkdown(text, emptyText = '暂无内容') {
  if (!text || !String(text).trim()) {
    return `<p class="md-empty">${emptyText}</p>`
  }
  return marked.parse(String(text))
}
