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

const ARCHIVE_NAME = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.tar\.gz$/

export const uploadProject = async (
  files: readonly File[],
  fileMatchers: readonly RegExp[],
  signal: AbortSignal
): Promise<string> => {
  const included = files.filter(
    file => !fileMatchers.some(matcher => file.webkitRelativePath.search(matcher) !== -1)
  )
  if (!included.length) throw new Error('The selected folder has no files to upload.')
  if (
    !included.some(
      file => file.webkitRelativePath.split('/').slice(1).join('/') === 'platformio.ini'
    )
  ) {
    throw new Error('Select a PlatformIO project folder containing platformio.ini at its root.')
  }

  const body = new FormData()
  included.forEach(file => body.append('files', file, file.webkitRelativePath))
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
