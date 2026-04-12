import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getSubjects, getQuestions, uploadQuestion } from '../api/adminApi';
import ExamResultsViewer from '../components/ExamResultsViewer';
import Spinner from '../components/Spinner';

const EMPTY_FORM = {
  questionText: '',
  options: ['', '', '', ''],
  correctOptionIndex: 0,
  difficulty: 'MEDIUM',
  marks: 1,
  negativeMarks: 0.25
};

export default function TeacherDashboard() {

  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [subjects, setSubjects] = useState([]);
  const [selectedSubject, setSelectedSubject] = useState(null);
  const [questions, setQuestions] = useState([]);

  const [form, setForm] = useState(EMPTY_FORM);

  const [loading, setLoading] = useState(false);
  const [uploadSuccess, setUploadSuccess] = useState(false);
  const [error, setError] = useState('');

  const [msg, setMsg] = useState('');
  const [teacherTab, setTeacherTab] = useState('Questions'); // 'Questions' | 'Results'
  const [questionFile, setQuestionFile] = useState(null);

  useEffect(() => {
    getSubjects().then(r => setSubjects(r.data.data || [])).catch(() => { });
  }, []);

  const loadQuestions = useCallback((subj) => {
    setLoading(true);
    getQuestions(subj.id)
      .then(r => setQuestions(r.data.data || []))
      .catch(() => setQuestions([]))
      .finally(() => setLoading(false));
  }, []);

  const selectSubject = (subj) => {
    setSelectedSubject(subj);
    setForm({ ...EMPTY_FORM, subjectId: subj.id });
    setUploadSuccess(false);
    setError('');
    loadQuestions(subj);
  };

  const setOption = (i, val) => {
    setForm(f => {
      const opts = [...f.options];
      opts[i] = val;
      return { ...f, options: opts };
    });
  };

  /* ---------------- MANUAL QUESTION UPLOAD ---------------- */

  const handleUpload = async (e) => {

    e.preventDefault();

    try {

      const payload = {
        ...form,
        subjectId: selectedSubject.id
      };

      const res = await uploadQuestion(payload);

      setUploadSuccess(true);
      setError('');
      setForm({ ...EMPTY_FORM, subjectId: selectedSubject.id });

      loadQuestions(selectedSubject);

    } catch (err) {

      setError('Failed to upload question');
      setUploadSuccess(false);

    }
  };

  /* ---------------- EXCEL UPLOAD ---------------- */

  const handleUploadQuestions = async (e) => {

    e.preventDefault();
    setMsg('');

    if (!questionFile) {
      setMsg('Please select an Excel file.');
      return;
    }

    try {

      const formData = new FormData();
      formData.append('file', questionFile);

      // Use axiosConfig so the JWT token is injected automatically
      // and the proxy routes correctly to the backend
      const stored = localStorage.getItem('examportal_user');
      const token = stored ? JSON.parse(stored).token : null;

      const res = await fetch('/questions/excel', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: formData
      });

      const data = await res.json().catch(() => ({}));

      if (!res.ok) {
        throw new Error(data.message || `Server error: ${res.status}`);
      }

      setMsg((data.message || 'Questions uploaded successfully.'));
      setQuestionFile(null);
      // Reset file input
      const fileInput = document.querySelector('input[type="file"]');
      if (fileInput) fileInput.value = '';

      if (selectedSubject) {
        loadQuestions(selectedSubject);
      }

    } catch (err) {
      setMsg('Upload failed: ' + (err.message || 'Unknown error. Check the file format.'));
    }
  };

  const inp = { width: '100%', padding: '8px 10px', border: '1px solid var(--border)', borderRadius: 5, fontSize: 13 };
  const lbl = { fontSize: 12, fontWeight: 700, color: 'var(--text-secondary)', display: 'block', marginBottom: 5 };

  return (

    <div style={{ minHeight: '100vh', background: 'var(--bg)', display: 'flex', flexDirection: 'column' }}>

      {/* NAVBAR */}

      <div style={{ background: '#fff', borderBottom: '1px solid var(--border)', height: 56, display: 'flex', alignItems: 'center', padding: '0 24px', justifyContent: 'space-between', boxShadow: 'var(--shadow-sm)' }}>

        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ fontSize: 20 }}>📝</span>
          <span style={{ fontWeight: 700, fontSize: 17, color: 'var(--primary)' }}>ExamPortal</span>
          <span style={{ marginLeft: 8, padding: '2px 8px', background: '#e0e7ff', color: '#4338ca', borderRadius: 4, fontSize: 11, fontWeight: 700 }}>
            TEACHER
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
            Welcome, <strong>{user?.fullName}</strong>
          </span>

          <button
            onClick={() => { logout(); navigate('/login'); }}
            style={{ padding: '6px 14px', border: '1px solid var(--border)', borderRadius: 6, background: '#fff', color: 'var(--text-secondary)', cursor: 'pointer', fontSize: 13 }}
          >
            Sign out
          </button>
        </div>

      </div>

      {/* Teacher tab bar */}
      <div style={{ background: '#fff', borderBottom: '1px solid var(--border)', padding: '0 24px', display: 'flex', gap: 0 }}>
        {['Questions', 'Results'].map(t => (
          <button key={t} onClick={() => setTeacherTab(t)}
            style={{
              padding: '12px 20px', border: 'none', borderBottom: teacherTab === t ? '2px solid var(--primary)' : '2px solid transparent',
              background: 'transparent', color: teacherTab === t ? 'var(--primary)' : 'var(--text-secondary)',
              fontWeight: teacherTab === t ? 700 : 400, cursor: 'pointer', fontSize: 14
            }}>
            {t === 'Questions' ? '📚 Questions' : '🏆 Results'}
          </button>
        ))}
      </div>

      {/* Results tab */}
      {teacherTab === 'Results' && (
        <div style={{ flex: 1, overflowY: 'auto', padding: 28 }}>
          <ExamResultsViewer />
        </div>
      )}

      {/* Questions tab */}
      {teacherTab === 'Questions' && (
        <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>

        {/* SUBJECT SIDEBAR */}

        <div style={{ width: 220, background: '#fff', borderRight: '1px solid var(--border)', overflowY: 'auto' }}>

          <div style={{ padding: '14px 16px', borderBottom: '1px solid var(--border)' }}>
            <h3 style={{ fontSize: 13, fontWeight: 700 }}>Subjects</h3>
          </div>

          {subjects.map(s => (

            <button
              key={s.id}
              onClick={() => selectSubject(s)}
              style={{
                width: '100%',
                textAlign: 'left',
                padding: '11px 16px',
                border: 'none',
                borderBottom: '1px solid var(--border-light)',
                cursor: 'pointer',
                background: selectedSubject?.id === s.id ? 'var(--primary-light)' : 'transparent'
              }}
            >
              <div>{s.name}</div>
              <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{s.code}</div>
            </button>

          ))}

        </div>

        {/* MAIN CONTENT */}

        <div style={{ flex: 1, overflowY: 'auto', padding: 24 }}>

          {!selectedSubject ? (

            <div style={{ textAlign: 'center', marginTop: 100 }}>
              <h3>Select a subject to manage questions</h3>
            </div>

          ) : (

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>

              {/* MANUAL QUESTION UPLOAD */}

              <div style={{ background: '#fff', borderRadius: 10, boxShadow: 'var(--shadow-sm)' }}>

                <div style={{ padding: 20 }}>
                  <h3>Upload Question — {selectedSubject.name}</h3>

                  <form onSubmit={handleUpload}>

                    <label style={lbl}>Question</label>

                    <textarea
                      value={form.questionText}
                      onChange={e => setForm(f => ({ ...f, questionText: e.target.value }))}
                      style={{ ...inp, marginBottom: 10 }}
                    />

                    {form.options.map((opt, i) => (
                      <input
                        key={i}
                        value={opt}
                        placeholder={`Option ${i + 1}`}
                        onChange={e => setOption(i, e.target.value)}
                        style={{ ...inp, marginBottom: 8 }}
                      />
                    ))}

                    <button
                      type="submit"
                      style={{ padding: 10, background: 'var(--primary)', color: '#fff', border: 'none', borderRadius: 5 }}
                    >
                      Upload Question
                    </button>

                  </form>

                </div>

                {/* EXCEL UPLOAD */}

                <div style={{ padding: 20, borderTop: '1px solid var(--border)' }}>

                  <h4 style={{ marginBottom: 12, fontSize: 14, fontWeight: 700 }}>Upload Questions via Excel</h4>

                  <div style={{ marginBottom: 8 }}>
                    <input
                      type="file"
                      accept=".xlsx"
                      onChange={(e) => { setQuestionFile(e.target.files[0]); setMsg(''); }}
                      style={{ fontSize: 13 }}
                    />
                    {questionFile && (
                      <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>
                        Selected: {questionFile.name} ({(questionFile.size/1024).toFixed(1)} KB)
                      </div>
                    )}
                  </div>

                  <button
                    onClick={handleUploadQuestions}
                    disabled={!questionFile}
                    style={{ padding: '9px 18px', background: 'var(--primary)', color: '#fff', border: 'none', borderRadius: 5, fontWeight: 600, cursor: questionFile ? 'pointer' : 'not-allowed', opacity: questionFile ? 1 : 0.5 }}
                  >
                    Upload Excel
                  </button>

                  {msg && (
                    <div style={{
                      marginTop: 10, padding: '8px 12px', borderRadius: 5, fontSize: 13,
                      background: msg.startsWith('✓') ? '#f0fdf4' : '#fef2f2',
                      color: msg.startsWith('✓') ? 'var(--success)' : 'var(--danger)',
                      border: '1px solid ' + (msg.startsWith('✓') ? '#bbf7d0' : '#fecaca')
                    }}>
                      {msg}
                    </div>
                  )}

                </div>

              </div>

              {/* QUESTION BANK */}

              <div style={{ background: '#fff', borderRadius: 10, boxShadow: 'var(--shadow-sm)' }}>

                <div style={{ padding: 20 }}>
                  <h3>Question Bank ({questions.length})</h3>
                </div>

                <div>

                  {loading ? (
                    <Spinner />
                  ) : (

                    questions.map((q, i) => (
                      <div key={q.id} style={{ padding: 15, borderTop: '1px solid var(--border-light)' }}>
                        <strong>Q{i + 1}</strong>
                        <p>{q.questionText}</p>
                      </div>
                    ))

                  )}

                </div>

              </div>

            </div>

          )}

        </div>

      </div>

      )}

    </div>

  );
}