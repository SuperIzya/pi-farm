export const DEFAULT_FILE_MATCHERS: readonly RegExp[] = [
  '__pycache__',
  'default_env.py',
  '\.pio',
  '\.vscode',
  '\.idea',
  '\.git',
  '\.svn',
  '\.hg',
  '\.lock',
  '\.DS_Store',
  '\.gitignore'
].map(s => new RegExp(s))

const globToRegExp = (glob: string) => {
  let expression = '^'
  for (let index = 0; index < glob.length; index += 1) {
    const char = glob[index]
    if (char === '*' && glob[index + 1] === '*') {
      index += 1
      if (glob[index + 1] === '/') {
        index += 1
        expression += '(?:.*/)?'
      } else expression += '.*'
    } else if (char === '*') expression += '[^/]*'
    else if (char === '?') expression += '[^/]'
    else if (char === '[') {
      const end = glob.indexOf(']', index + 1)
      if (end < 0) throw new Error(`Invalid code file glob: ${glob}`)
      const characterClass = glob.slice(index + 1, end)
      expression += `[${characterClass.startsWith('!') ? `^${characterClass.slice(1)}` : characterClass}]`
      index = end
    } else expression += char.replace(/[|\\{}()[\]^$+?.]/g, '\\$&')
  }
  return new RegExp(`${expression}$`)
}

const ARCHIVE_NAME = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.tar\.gz$/

export const uploadProject = async (
  files: readonly File[],
  codeFileGlobs: readonly string[],
  fileMatchers: readonly RegExp[],
  signal: AbortSignal
): Promise<string> => {
  const codeMatchers = codeFileGlobs.map(globToRegExp)
  const projectPath = (path: string) => path.split('/').slice(1).join('/')
  const matchesCodeFile = (path: string) => codeMatchers.some(matcher => matcher.test(path))
  const included = files.filter(file => {
    const path = projectPath(file.webkitRelativePath)
    return (
      !fileMatchers.some(matcher => matcher.test(file.webkitRelativePath)) && matchesCodeFile(path)
    )
  })
  if (!included.length) throw new Error('The selected folder has no files to upload.')
  if (!included.some(file => projectPath(file.webkitRelativePath) === 'platformio.ini')) {
    throw new Error('Select a PlatformIO project folder containing platformio.ini at its root.')
  }
  const body = new FormData()
  included.forEach(file => body.append('files', file, projectPath(file.webkitRelativePath)))
  const response = await fetch('/api/code', {
    method: 'POST',
    body,
    signal: AbortSignal.any([signal, AbortSignal.timeout(5 * 60 * 1000)])
  })
  const result = (await response.text()).trim()
  if (!response.ok) throw new Error(result || `Project upload failed (HTTP ${response.status}).`)
  if (!ARCHIVE_NAME.test(result)) throw new Error('The server returned an invalid archive name.')
  return result
}
