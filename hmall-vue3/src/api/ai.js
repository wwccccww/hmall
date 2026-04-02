import request from '@/utils/request'

/** AI 对话（同步） */
export function aiChatSync(message, context) {
  return request.post('/ai/chat/sync', { message, context })
}

/**
 * AI 对话（SSE 流式）
 * 注意：浏览器原生 EventSource 不支持 POST，这里用 fetch + ReadableStream 解析 SSE。
 */
export async function aiChatStream(message, { context, onDelta, onSources, onDone, onError } = {}) {
  try {
    const token = sessionStorage.getItem('token') || ''
    const res = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { authorization: token } : {})
      },
      body: JSON.stringify({ message, context: context || null })
    })

    if (!res.ok) {
      const text = await res.text().catch(() => '')
      throw new Error(`HTTP ${res.status} ${text}`)
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buf = ''

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buf.indexOf('\n\n')) >= 0) {
        const rawEvent = buf.slice(0, idx)
        buf = buf.slice(idx + 2)
        handleSseEvent(rawEvent, { onDelta, onSources, onDone })
      }
    }

    onDone?.()
  } catch (e) {
    onError?.(e)
    throw e
  }
}

function handleSseEvent(rawEvent, { onDelta, onSources, onDone }) {
  // SSE event format:
  // event: message
  // data: {"delta":"..."}
  let eventName = 'message'
  const lines = rawEvent.split('\n')
  let dataLines = []
  for (const line of lines) {
    if (line.startsWith('event:')) eventName = line.slice(6).trim()
    if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
  }
  const dataStr = dataLines.join('\n')
  if (!dataStr) return

  let payload
  try {
    payload = JSON.parse(dataStr)
  } catch {
    payload = { raw: dataStr }
  }

  if (eventName === 'message') {
    const delta = payload?.delta ?? ''
    if (delta) onDelta?.(delta)
  } else if (eventName === 'sources') {
    onSources?.(payload?.sources || payload)
  } else if (eventName === 'done') {
    onDone?.()
  }
}

