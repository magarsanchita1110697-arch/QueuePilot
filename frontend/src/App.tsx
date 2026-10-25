
import { useEffect, useState, type FormEvent } from 'react'
import './App.css'

type JobStatus = 'QUEUED' | 'RUNNING' | 'RETRY_WAIT' | 'SUCCEEDED' | 'FAILED'

type Job = {
  id: string
  jobType: string
  payload: Record<string, unknown>
  status: JobStatus
  attemptCount: number
  maxAttempts: number
  lastError: string | null
  createdAt: string
}

function formatDate(value: string) {
  return new Date(value).toLocaleString([], {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

function App() {
  const [jobs, setJobs] = useState<Job[]>([])
  const [loading, setLoading] = useState(true)
  const [apiOnline, setApiOnline] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [jobType, setJobType] = useState('REPORT_GENERATION')
  const [reportName, setReportName] = useState('weekly-summary')
  const [maxAttempts, setMaxAttempts] = useState(3)

  async function loadJobs() {
    setLoading(true)
    setError('')

    try {
      const healthResponse = await fetch('/api/health')
      if (!healthResponse.ok) {
        throw new Error('QueuePilot API is unavailable.')
      }

      setApiOnline(true)

      const response = await fetch('/api/jobs')
      if (!response.ok) {
        throw new Error('Could not retrieve jobs.')
      }

      const data: Job[] = await response.json()
      setJobs(data)
    } catch (err) {
      setApiOnline(false)
      setError(err instanceof Error ? err.message : 'Something went wrong.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadJobs()
  }, [])

  async function createJob(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    setNotice('')

    try {
      const response = await fetch('/api/jobs', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          jobType,
          payload: { reportName: reportName.trim() },
          maxAttempts,
        }),
      })

      if (!response.ok) {
        throw new Error(
          response.status === 400
            ? 'Please check the job details and try again.'
            : `Job creation failed (${response.status}).`,
        )
      }

      const created: Job = await response.json()
      setNotice(`Job created successfully · ${created.id.slice(0, 8)}`)
      setReportName('')
      await loadJobs()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not create job.')
    } finally {
      setSubmitting(false)
    }
  }

  const queued = jobs.filter(
    (job) => job.status === 'QUEUED' || job.status === 'RETRY_WAIT',
  ).length
  const running = jobs.filter((job) => job.status === 'RUNNING').length
  const succeeded = jobs.filter((job) => job.status === 'SUCCEEDED').length
  const failed = jobs.filter((job) => job.status === 'FAILED').length

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#overview">
          <span className="brand-mark">Q</span>
          <span>QueuePilot<small>JOB ORCHESTRATION</small></span>
        </a>

        <div className="nav-label">WORKSPACE</div>
        <a className="nav-item active" href="#overview">
          <span>◫</span> Overview
        </a>
        <a className="nav-item" href="#jobs">
          <span>☷</span> Jobs
          <span className="nav-count">{jobs.length}</span>
        </a>

        <div className="sidebar-bottom">
          <div className="sidebar-status">
            <span className={`pulse ${apiOnline ? 'online' : ''}`} />
            <div>
              <strong>{apiOnline ? 'API connected' : 'API disconnected'}</strong>
              <small>Local environment</small>
            </div>
          </div>
          <div className="version-label">QueuePilot · MVP</div>
        </div>
      </aside>

      <main className="main-content" id="overview">
        <header className="topbar">
          <div className="breadcrumb">Workspace <span>/</span> Overview</div>
          <div className="topbar-right">
            <span className="environment"><span /> DEVELOPMENT</span>
            <button className="refresh-button" onClick={() => void loadJobs()}>
              ↻ <span>Refresh</span>
            </button>
            <div className="avatar">S</div>
          </div>
        </header>

        <section className="page-heading">
          <div>
            <div className="eyebrow">COMMAND CENTER</div>
            <h1>Good to see you.</h1>
            <p>Monitor your jobs, track execution, and keep work moving.</p>
          </div>
          <button
            className="primary-button"
            onClick={() =>
              document.getElementById('create-job')?.scrollIntoView({
                behavior: 'smooth',
                block: 'center',
              })
            }
          >
            <span>＋</span> Create a job
          </button>
        </section>

        {!apiOnline && (
          <div className="alert error-alert">
            <strong>Backend unavailable</strong>
            <span>Start the Spring Boot backend on port 8080, then refresh.</span>
          </div>
        )}

        {error && (
          <div className="alert error-alert">
            <strong>Something went wrong</strong>
            <span>{error}</span>
          </div>
        )}

        {notice && <div className="alert success-alert">{notice}</div>}

        <section className="stats-grid">
          <article className="stat-card">
            <div className="stat-top"><span>Total jobs</span><span className="stat-icon">▤</span></div>
            <div className="stat-number">{jobs.length}</div>
            <div className="stat-caption">All recorded jobs</div>
          </article>
          <article className="stat-card">
            <div className="stat-top"><span>Queued</span><span className="stat-icon amber">◷</span></div>
            <div className="stat-number">{queued}</div>
            <div className="stat-caption">Waiting for execution</div>
          </article>
          <article className="stat-card">
            <div className="stat-top"><span>Running</span><span className="stat-icon blue">↗</span></div>
            <div className="stat-number">{running}</div>
            <div className="stat-caption">Currently processing</div>
          </article>
          <article className="stat-card">
            <div className="stat-top"><span>Failed</span><span className="stat-icon red">!</span></div>
            <div className="stat-number">{failed}</div>
            <div className="stat-caption">{succeeded} succeeded so far</div>
          </article>
        </section>

        <div className="content-grid">
          <section className="panel jobs-panel" id="jobs">
            <div className="panel-heading">
              <div>
                <h2>Recent jobs</h2>
                <p>Your persisted job records</p>
              </div>
              <span className="record-count">{jobs.length} records</span>
            </div>

            {loading ? (
              <div className="empty-state"><div className="loader" /> Loading jobs…</div>
            ) : jobs.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">▤</div>
                <strong>No jobs yet</strong>
                <span>Create your first job using the form.</span>
              </div>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>JOB</th>
                      <th>STATUS</th>
                      <th>ATTEMPTS</th>
                      <th>CREATED</th>
                    </tr>
                  </thead>
                  <tbody>
                    {jobs.map((job) => (
                      <tr key={job.id}>
                        <td>
                          <div className="job-name">{job.jobType.replaceAll('_', ' ')}</div>
                          <div className="job-id">{job.id.slice(0, 8)}…</div>
                        </td>
                        <td>
                          <span className={`status status-${job.status.toLowerCase()}`}>
                            <span /> {job.status.replaceAll('_', ' ')}
                          </span>
                        </td>
                        <td>{job.attemptCount} / {job.maxAttempts}</td>
                        <td className="date-cell">{formatDate(job.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="panel-footer">
              <span>Data from PostgreSQL via the QueuePilot API</span>
              <button onClick={() => void loadJobs()}>Refresh list ↻</button>
            </div>
          </section>

          <section className="panel create-panel" id="create-job">
            <div className="panel-heading">
              <div>
                <div className="eyebrow">NEW WORKFLOW</div>
                <h2>Create a job</h2>
                <p>Submit work to the QueuePilot API.</p>
              </div>
              <span className="create-icon">＋</span>
            </div>

            <form onSubmit={createJob}>
              <label htmlFor="jobType">Job type</label>
              <select
                id="jobType"
                value={jobType}
                onChange={(event) => setJobType(event.target.value)}
              >
                <option value="REPORT_GENERATION">Report generation</option>
                <option value="DATA_EXPORT">Data export</option>
                <option value="NOTIFICATION">Notification</option>
              </select>

              <label htmlFor="reportName">Payload · report name</label>
              <input
                id="reportName"
                value={reportName}
                onChange={(event) => setReportName(event.target.value)}
                placeholder="e.g. weekly-summary"
                required
              />

              <label htmlFor="maxAttempts">Maximum attempts</label>
              <select
                id="maxAttempts"
                value={maxAttempts}
                onChange={(event) => setMaxAttempts(Number(event.target.value))}
              >
                {[1, 2, 3, 4, 5].map((count) => (
                  <option key={count} value={count}>{count} {count === 1 ? 'attempt' : 'attempts'}</option>
                ))}
              </select>

              <div className="form-note">
                <span>ⓘ</span>
                Jobs are saved to PostgreSQL with an initial QUEUED status.
              </div>

              <button className="primary-button submit-button" disabled={submitting || !apiOnline}>
                {submitting ? 'Submitting…' : '＋  Submit job'}
              </button>
            </form>
          </section>
        </div>

        <section className="insight-banner">
          <div className="insight-mark">✳</div>
          <div>
            <strong>Failure intelligence</strong>
            <p>AI-assisted failure investigation is planned for a later milestone. Job execution and retry processing are not yet implemented.</p>
          </div>
          <span className="planned-badge">ROADMAP</span>
        </section>

        <footer className="app-footer">
          <span>QueuePilot <span className="footer-dot">·</span> Job orchestration platform</span>
          <span><span className={`pulse ${apiOnline ? 'online' : ''}`} /> {apiOnline ? 'Connected to local API' : 'Waiting for API connection'}</span>
        </footer>
      </main>
    </div>
  )
}

export default App
