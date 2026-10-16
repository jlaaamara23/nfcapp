import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { api, type MetaItem, type Station } from '../api/client';

export function StationsPage() {
  const [stations, setStations] = useState<Station[]>([]);
  const [workplaces, setWorkplaces] = useState<MetaItem[]>([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ stationCode: '', name: '', workplaceId: '' });

  async function load() {
    const [s, w] = await Promise.all([api.stations(), api.workplaces()]);
    setStations(s);
    setWorkplaces(w);
    if (!form.workplaceId && w[0]) setForm((f) => ({ ...f, workplaceId: String(w[0].id) }));
  }

  useEffect(() => {
    load().catch((e: Error) => setError(e.message));
  }, []);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    await api.createStation({
      stationCode: form.stationCode,
      name: form.name,
      workplaceId: Number(form.workplaceId),
    });
    setForm({ stationCode: '', name: '', workplaceId: form.workplaceId });
    await load();
  }

  return (
    <section className="page">
      <header className="page-header">
        <h1>ניהול תחנות NFC</h1>
        <p>
          תג NTAG מכיל מזהה תחנה בלבד — לא סוד זהות. האפליקציה שולחת את המזהה יחד עם JWT מאומת.
          להגנה מחזקה יותר מומלץ תגיות קריפטוגרפיות (NTAG 424 DNA), אתגור מסתובב ו-geo-fencing.
        </p>
      </header>
      <form className="filters panel" onSubmit={onSubmit}>
        <input
          required
          placeholder="קוד תחנה (GATE-02)"
          value={form.stationCode}
          onChange={(e) => setForm({ ...form, stationCode: e.target.value })}
        />
        <input
          required
          placeholder="שם תחנה"
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
        />
        <select required value={form.workplaceId} onChange={(e) => setForm({ ...form, workplaceId: e.target.value })}>
          {workplaces.map((w) => (
            <option key={w.id} value={w.id}>
              {w.name}
            </option>
          ))}
        </select>
        <button type="submit">רישום תחנה</button>
      </form>
      {error && <div className="error">{error}</div>}
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>קוד</th>
              <th>שם</th>
              <th>מיקום</th>
              <th>ערך רישום</th>
              <th>Payload לתג</th>
              <th>סטטוס</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {stations.map((s) => (
              <tr key={s.id}>
                <td>{s.stationCode}</td>
                <td>{s.name}</td>
                <td>{s.workplaceName}</td>
                <td>
                  <code>{s.registrationValue}</code>
                </td>
                <td>
                  <code className="wrap">{s.nfcPayload}</code>
                </td>
                <td>{s.status}</td>
                <td className="actions">
                  <button
                    type="button"
                    onClick={() =>
                      void api
                        .updateStationStatus(s.id, {
                          status: s.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE',
                        })
                        .then(load)
                    }
                  >
                    {s.status === 'ACTIVE' ? 'השבתה' : 'הפעלה'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
