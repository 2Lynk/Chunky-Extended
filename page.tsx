"use client"

import Link from 'next/link'
import Navbar from '@/components/Navbar'
import { useState } from 'react'

const whatItDoes = [
  'Adds server-side automation controls on top of Chunky.',
  'Provides `/chunky-extend` commands to enable, disable, and check automation status.',
  'When automation is enabled, resumes Chunky when the last player disconnects.',
  'Adds optional scheduled pause/continue rules with timezone support.',
  'Persists full settings and schedule rules in `ce.json` across restarts.'
]

const quickStart = [
  'Install Chunky first and verify `/chunky` commands are available.',
  'Install Chunky Extended for your loader and game version.',
  'Start server, run `/chunky-extend enable`, then run your normal Chunky generation command.',
  'Optionally set schedule timezone with `/chunky-extend settings timezone Europe/Amsterdam`.',
  'Use the quick nightly preset via `/chunky-extend schedule preset nightly mon-fri`.',
  'Or use weekend defaults via `/chunky-extend schedule preset weekend`.',
  'Add scheduler rules such as `/chunky-extend schedule add 01:00 continue` and `/chunky-extend schedule add 07:00 pause mon-fri`.',
  'Or create both rules at once with `/chunky-extend schedule preset window 01:00 07:00 mon-fri`.',
  'Run `/chunky-extend schedule next` to preview the next scheduled action.',
  'Check for new releases with `/chunky-extend update check`.',
  'Use `/chunky-extend status` anytime to confirm automation state.'
]

const commands = [
  {
    cmd: '/chunky-extend',
    desc: 'Shows command help in chat.'
  },
  {
    cmd: '/chunky-extend enable',
    desc: 'Enables automatic Chunky resume behavior when everyone leaves.'
  },
  {
    cmd: '/chunky-extend disable',
    desc: 'Disables automation and keeps Chunky behavior fully manual.'
  },
  {
    cmd: '/chunky-extend status',
    desc: 'Prints whether Chunky Extended automation is currently enabled or disabled.'
  },
  {
    cmd: '/chunky-extend update check',
    desc: 'Checks Modrinth for a newer Chunky Extended version.'
  },
  {
    cmd: '/chunky-extend settings',
    desc: 'Shows all current settings including scheduler flags and timezone.'
  },
  {
    cmd: '/chunky-extend settings <autopause|autocontinue|scheduler|skip-online> <true|false>',
    desc: 'Updates automation behavior toggles.'
  },
  {
    cmd: '/chunky-extend settings timezone <ZoneId>',
    desc: 'Sets scheduler timezone, e.g. `Europe/Amsterdam` or `UTC`.'
  },
  {
    cmd: '/chunky-extend schedule add <HH:mm> <pause|continue> [days]',
    desc: 'Adds schedule rules; days can be `all`, `mon,wed,fri`, or ranges like `mon-fri`.'
  },
  {
    cmd: '/chunky-extend schedule list | remove <id> | clear | enable | disable',
    desc: 'Manages stored scheduler rules and scheduler state.'
  },
  {
    cmd: '/chunky-extend schedule next',
    desc: 'Shows the next upcoming schedule trigger in your configured timezone.'
  },
  {
    cmd: '/chunky-extend schedule preset window <start> <end> [days]',
    desc: 'Adds a paired window: `continue` at start and `pause` at end (great for nightly jobs).'
  },
  {
    cmd: '/chunky-extend schedule preset nightly [days]',
    desc: 'Shortcut preset for `01:00 continue` and `07:00 pause`.'
  },
  {
    cmd: '/chunky-extend schedule preset weekend [days]',
    desc: 'Weekend-oriented preset (defaults to `sat,sun`) with `01:00 continue` and `07:00 pause`.'
  }
]

const faq = [
  {
    q: 'Is Chunky required as well?',
    a: 'Yes. Chunky Extension builds on top of Chunky and is not a standalone replacement.'
  },
  {
    q: 'What Minecraft versions are supported?',
    a: 'This build targets the 26.1 line: 26.1, 26.1.1, and 26.1.2.'
  },
  {
    q: 'How should I run pregeneration on a live server?',
    a: 'Use smaller generation batches, monitor TPS/tick time, and schedule long runs during low player activity.'
  },
  {
    q: 'What extra settings are available?',
    a: 'You can toggle auto-pause on first join, auto-continue on last leave, scheduler on/off, and skip scheduled actions while players are online.'
  }
]

function renderInlineCode(text: string) {
  return text.split(/(`[^`]+`)/g).map((part, index) => {
    if (part.startsWith('`') && part.endsWith('`')) {
      return (
        <code
          key={`${part}-${index}`}
          style={{
            fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
            background: 'rgba(255,255,255,0.08)',
            border: '1px solid var(--border)',
            borderRadius: 6,
            padding: '2px 6px',
            fontSize: '0.92em'
          }}
        >
          {part.slice(1, -1)}
        </code>
      )
    }

    return <span key={`${part}-${index}`}>{part}</span>
  })
}

export default function ChunkyExtensionDocsPage() {
  const [copiedCommand, setCopiedCommand] = useState<string | null>(null)

  const copyCommand = async (command: string) => {
    if (typeof navigator === 'undefined' || !navigator.clipboard) {
      return
    }

    try {
      await navigator.clipboard.writeText(command)
      setCopiedCommand(command)
      setTimeout(() => {
        setCopiedCommand((current: string | null) => (current === command ? null : current))
      }, 1400)
    } catch {
    }
  }

  return (
    <main>
      <div className="gradient-bg" />
      <Navbar />

      <section className="section-shell section" style={{ paddingTop: 72 }}>
        <span className="eyebrow">Mod reference · Setup and operations</span>
        <h1 className="gradient-text hero-title" style={{ marginTop: 14 }}>
          Chunky Extension Docs
        </h1>
        <p className="lead" style={{ marginInline: 0, maxWidth: 860 }}>
          Chunky Extension is a lightweight helper mod for Chunky that adds automation controls for
          pre-generation workflows on multiplayer servers.
        </p>

        <div className="feature-grid" style={{ marginTop: 24 }}>
          <article className="card">
            <h2 style={{ marginTop: 0 }}>What it does</h2>
            <ol style={{ margin: 0, paddingLeft: 20, lineHeight: 1.75, opacity: 0.84 }}>
              {whatItDoes.map((step) => (
                <li key={step}>{renderInlineCode(step)}</li>
              ))}
            </ol>
          </article>

          <article className="card">
            <h2 style={{ marginTop: 0 }}>Quick start</h2>
            <ul style={{ margin: 0, paddingLeft: 20, lineHeight: 1.75, opacity: 0.84 }}>
              {quickStart.map((item) => (
                <li key={item}>{renderInlineCode(item)}</li>
              ))}
            </ul>
          </article>
        </div>

        <article className="card" style={{ marginTop: 20 }}>
          <h2 style={{ marginTop: 0 }}>Command reference</h2>
          <p style={{ marginTop: 0, opacity: 0.68 }}>Use the Copy button to paste commands directly in-game or in your server console.</p>
          <div style={{ display: 'grid', gap: 12 }}>
            {commands.map((item) => (
              <div key={item.cmd} style={{ border: '1px solid var(--border)', borderRadius: 12, padding: 14 }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
                  <p style={{ margin: 0, fontWeight: 700, fontFamily: 'monospace' }}>{renderInlineCode(item.cmd)}</p>
                  <button
                    type="button"
                    onClick={() => copyCommand(item.cmd)}
                    style={{
                      border: '1px solid var(--border)',
                      borderRadius: 8,
                      padding: '6px 10px',
                      background: copiedCommand === item.cmd ? 'rgba(120,255,180,0.16)' : 'rgba(255,255,255,0.04)',
                      color: 'var(--text)',
                      fontSize: '0.85rem',
                      cursor: 'pointer'
                    }}
                  >
                    {copiedCommand === item.cmd ? 'Copied!' : 'Copy'}
                  </button>
                </div>
                <p style={{ margin: '8px 0 0', opacity: 0.76, lineHeight: 1.66 }}>{renderInlineCode(item.desc)}</p>
              </div>
            ))}
          </div>
        </article>

        <article className="card" style={{ marginTop: 20 }}>
          <h2 style={{ marginTop: 0 }}>How to use it in practice</h2>
          <p style={{ opacity: 0.76, lineHeight: 1.72 }}>
            Treat Chunky generation like a scheduled operation: enable automation, run generation in
            stages, and watch server health while jobs run. This keeps world prep predictable and reduces
            player-facing performance spikes.
          </p>
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <span className="chip">Depends on Chunky</span>
            <span className="chip">Automation toggle command</span>
            <span className="chip">Timezone-aware scheduler</span>
            <span className="chip">Best with staged pregeneration</span>
          </div>
        </article>

        <article className="card" style={{ marginTop: 20 }}>
          <h2 style={{ marginTop: 0 }}>FAQ</h2>
          <div style={{ display: 'grid', gap: 12 }}>
            {faq.map((item) => (
              <div key={item.q} style={{ border: '1px solid var(--border)', borderRadius: 12, padding: 14 }}>
                <p style={{ margin: 0, fontWeight: 700 }}>{renderInlineCode(item.q)}</p>
                <p style={{ margin: '8px 0 0', opacity: 0.76, lineHeight: 1.66 }}>{renderInlineCode(item.a)}</p>
              </div>
            ))}
          </div>
        </article>

        <div style={{ marginTop: 22, display: 'flex', gap: 14, flexWrap: 'wrap' }}>
          <a href="https://modrinth.com/project/LFJf0Klb" target="_blank" rel="noreferrer" className="glow-btn" style={{ textDecoration: 'none' }}>
            View on Modrinth
          </a>
          <Link
            href="/docs"
            className="glow-btn"
            style={{
              textDecoration: 'none',
              background: 'transparent',
              border: '1px solid var(--border)',
              boxShadow: 'none'
            }}
          >
            Back to docs hub
          </Link>
        </div>
      </section>
    </main>
  )
}
