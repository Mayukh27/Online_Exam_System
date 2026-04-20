import React, { useEffect, useState, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getSubjects, getQuestions } from '../api/adminApi';
import api from '../api/axiosConfig';
import ExamResultsViewer from '../components/ExamResultsViewer';
import Spinner from '../components/Spinner';
import { runLogoutFlow } from '../utils/authSession';

const EMPTY_FORM = {
  questionText: '',
  options: ['', '', '', ''],
  correctOptionIndex: 0,
  difficulty: 'MEDIUM',
  marks: 1,
  negativeMarks: 0.25,
};

/** Small image preview thumbnail */
function ImageThumb({ file, onRemove, label }) {
  const [url, setUrl] = useState(null);
  useEffect(() => {
    if (!file) { setUrl(null); return; }
    const u = URL.createObjectURL(file);
    setUrl(u);
    return () => URL.revokeObjectURL(u);
  }, [file]);

  if (!url) return null;
  return (
    <div style={{ display: 'inline-flex', flexDirection: 'column', alignItems: 'center', gap: 2, marginTop: 4 }}>
      <img src={url} alt={label} style={{ width: 64, height: 64, objectFit: 'cover', borderRadius: 4, border: '1px solid #cbd5e1' }} />
      <button type="button" onClick={onRemove}
        style={{ fontSize: 10, padding: '1px 6px', background: '#fee2e2', color: '#b91c1c', border: 'none', borderRadius: 3, cursor: 'pointer' }}>
        ✕ Remove
      </button>
    </div>
  );
}

export default function TeacherDashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [subjects, setSubjects] = useState([]);
  const [selectedSubject, setSelectedSubject] = useState(null);
  const [questions, setQuestions] = useState([]);

  const [form, setForm] = useState(EMPTY_FORM);

  // Image state
  const [questionImageFile, setQuestionImageFile] = useState(null);
  const [optionImageFiles, setOptionImageFiles] = useState([null, null, null, null]);
  const qImgInputRef = useRef(null);
  const optImgRefs = [useRef(null), useRef(null), useRef(null), useRef(null)];

  const [loading, setLoading] = useState(false);
  const [uploadSuccess, setUploadSuccess] = useState(false);
  const [error, setError] = useState('');

  const [msg, setMsg] = useState('');
  const [teacherTab, setTeacherTab] = useState('Questions');
  const [questionFile, setQuestionFile] = useState(null);
  const [imageZipFile, setImageZipFile] = useState(null);
  const excelInputRef = useRef(null);
  const imageZipInputRef = useRef(null);

  useEffect(() => {
    const handleBack = async () => {
      await runLogoutFlow({
        apiClient: api,
        logout,
        onError: () => console.warn('Logout failed')
      });
      navigate('/login', { replace: true });
    };

    window.history.pushState(null, '', window.location.href);
    window.addEventListener('popstate', handleBack);

    return () => {
      window.removeEventListener('popstate', handleBack);
    };
  }, [logout, navigate]);

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
    resetImages();
    setUploadSuccess(false);
    setError('');
    loadQuestions(subj);
  };

  const resetImages = () => {
    setQuestionImageFile(null);
    setOptionImageFiles([null, null, null, null]);
    if (qImgInputRef.current) qImgInputRef.current.value = '';
    optImgRefs.forEach(r => { if (r.current) r.current.value = ''; });
  };

  const setOption = (i, val) => {
    setForm(f => {
      const opts = [...f.options];
      opts[i] = val;
      return { ...f, options: opts };
    });
  };

  const setOptionImage = (i, file) => {
    setOptionImageFiles(prev => {
      const next = [...prev];
      next[i] = file;
      return next;
    });
  };

  /* ─── MANUAL QUESTION UPLOAD (multipart) ─── */
  const handleUpload = async (e) => {
    e.preventDefault();
    setError('');
    setUploadSuccess(false);

    if (!selectedSubject) { setError('Select a subject first.'); return; }
    if (form.options.filter(o => o.trim()).length < 2) {
      setError('Please enter at least 2 options.'); return;
    }

    try {
      const fd = new FormData();
      fd.append('subjectId', selectedSubject.id);
      fd.append('questionText', form.questionText);
      form.options.forEach(opt => fd.append('options', opt));
      fd.append('correctOptionIndex', form.correctOptionIndex);
      fd.append('difficulty', form.difficulty);
      fd.append('marks', form.marks);
      fd.append('negativeMarks', form.negativeMarks);

      if (questionImageFile) fd.append('questionImage', questionImageFile);
      optionImageFiles.forEach((f, i) => { if (f) fd.append(`optionImage_${i}`, f); });

      const res = await api.post('/questions/upload', fd);
      const data = res?.data || {};

      setUploadSuccess(true);
      setForm({ ...EMPTY_FORM, subjectId: selectedSubject.id });
      resetImages();
      loadQuestions(selectedSubject);

    } catch (err) {
      setError('Failed to upload question: ' + (err.message || 'Unknown error'));
    }
  };

  /* ─── EXCEL UPLOAD ─── */
  const handleUploadQuestions = async (e) => {
    e.preventDefault();
    setMsg('');
    if (!questionFile) { setMsg('Please select an Excel or ZIP file.'); return; }

    const primaryName = (questionFile.name || '').toLowerCase();
    const isBundleZip = primaryName.endsWith('.zip');
    const isExcel = primaryName.endsWith('.xlsx');

    if (!isBundleZip && !isExcel) {
      setMsg('Upload failed: primary file must be .xlsx or .zip');
      return;
    }

    if (isBundleZip && imageZipFile) {
      setMsg('Upload failed: when primary file is a ZIP bundle, do not attach image ZIP separately.');
      return;
    }

    if (imageZipFile && !imageZipFile.name.toLowerCase().endsWith('.zip')) {
      setMsg('Upload failed: image ZIP must be a .zip file.');
      return;
    }

    try {
      const formData = new FormData();
      formData.append('file', questionFile);
      if (imageZipFile) formData.append('imageZip', imageZipFile);

      const res = await api.post('/questions/excel', formData);
      const data = res?.data || {};

      setMsg('✓ ' + (data.message || 'Questions uploaded successfully.'));
      setQuestionFile(null);
      setImageZipFile(null);
      if (excelInputRef.current) excelInputRef.current.value = '';
      if (imageZipInputRef.current) imageZipInputRef.current.value = '';
      if (selectedSubject) loadQuestions(selectedSubject);

    } catch (err) {
      setMsg('Upload failed: ' + (err.message || 'Check the file format.'));
    }
  };

  const inp = { width: '100%', padding: '8px 10px', border: '1px solid var(--border)', borderRadius: 5, fontSize: 13, boxSizing: 'border-box' };
  const lbl = { fontSize: 12, fontWeight: 700, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 };
  const sec = { marginBottom: 14 };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)', display: 'flex', flexDirection: 'column' }}>

      {/* NAVBAR */}
      <div style={{ background: '#fff', borderBottom: '1px solid var(--border)', height: 56, display: 'flex', alignItems: 'center', padding: '0 24px', justifyContent: 'space-between', boxShadow: 'var(--shadow-sm)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ fontSize: 20 }}>📝</span>
          <span style={{ fontWeight: 700, fontSize: 17, color: 'var(--primary)' }}>ExamPortal</span>
          <span style={{ marginLeft: 8, padding: '2px 8px', background: '#e0e7ff', color: '#4338ca', borderRadius: 4, fontSize: 11, fontWeight: 700 }}>TEACHER</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>Welcome, <strong>{user?.fullName}</strong></span>
          <button onClick={async () => {
            await runLogoutFlow({
              apiClient: api,
              logout,
              onError: () => console.warn('Logout failed')
            });
            navigate('/login');
          }}
            style={{ padding: '6px 14px', border: '1px solid var(--border)', borderRadius: 6, background: '#fff', color: 'var(--text-secondary)', cursor: 'pointer', fontSize: 13 }}>
            Sign out
          </button>
        </div>
      </div>

      {/* TAB BAR */}
      <div style={{ background: '#fff', borderBottom: '1px solid var(--border)', padding: '0 24px', display: 'flex' }}>
        {['Questions', 'Results'].map(t => (
          <button key={t} onClick={() => setTeacherTab(t)}
            style={{ padding: '12px 20px', border: 'none', borderBottom: teacherTab === t ? '2px solid var(--primary)' : '2px solid transparent', background: 'transparent', color: teacherTab === t ? 'var(--primary)' : 'var(--text-secondary)', fontWeight: teacherTab === t ? 700 : 400, cursor: 'pointer', fontSize: 14 }}>
            {t === 'Questions' ? '📚 Questions' : '🏆 Results'}
          </button>
        ))}
      </div>

      {/* RESULTS TAB */}
      {teacherTab === 'Results' && (
        <div style={{ flex: 1, overflowY: 'auto', padding: 28 }}>
          <ExamResultsViewer />
        </div>
      )}

      {/* QUESTIONS TAB */}
      {teacherTab === 'Questions' && (
        <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>

          {/* SUBJECT SIDEBAR */}
          <div style={{ width: 220, background: '#fff', borderRight: '1px solid var(--border)', overflowY: 'auto' }}>
            <div style={{ padding: '14px 16px', borderBottom: '1px solid var(--border)' }}>
              <h3 style={{ fontSize: 13, fontWeight: 700 }}>Subjects</h3>
            </div>
            {subjects.map(s => (
              <button key={s.id} onClick={() => selectSubject(s)}
                style={{ width: '100%', textAlign: 'left', padding: '11px 16px', border: 'none', borderBottom: '1px solid var(--border-light)', cursor: 'pointer', background: selectedSubject?.id === s.id ? 'var(--primary-light)' : 'transparent' }}>
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

                {/* ── MANUAL UPLOAD FORM ── */}
                <div style={{ background: '#fff', borderRadius: 10, boxShadow: 'var(--shadow-sm)' }}>
                  <div style={{ padding: 20 }}>
                    <h3 style={{ marginBottom: 16, fontSize: 15 }}>Upload Question — {selectedSubject.name}</h3>

                    <form onSubmit={handleUpload}>

                      {/* Question text */}
                      <div style={sec}>
                        <label style={lbl}>Question Text (optional if image provided)</label>
                        <textarea
                          value={form.questionText}
                          onChange={e => setForm(f => ({ ...f, questionText: e.target.value }))}
                          rows={3}
                          style={{ ...inp, resize: 'vertical' }}
                        />
                      </div>

                      {/* Question image */}
                      <div style={sec}>
                        <label style={lbl}>Question Image (optional)</label>
                        <input ref={qImgInputRef} type="file" accept="image/*"
                          onChange={e => setQuestionImageFile(e.target.files[0] || null)}
                          style={{ fontSize: 12 }} />
                        <ImageThumb file={questionImageFile} label="Question image"
                          onRemove={() => { setQuestionImageFile(null); if (qImgInputRef.current) qImgInputRef.current.value = ''; }} />
                      </div>

                      {/* Options */}
                      <div style={sec}>
                        <label style={lbl}>Options (text and/or image per option)</label>
                        {form.options.map((opt, i) => (
                          <div key={i} style={{ marginBottom: 10, padding: 10, border: '1px solid var(--border)', borderRadius: 6, background: form.correctOptionIndex === i ? '#f0fdf4' : '#fafafa' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
                              <input type="radio" name="correct" checked={form.correctOptionIndex === i}
                                onChange={() => setForm(f => ({ ...f, correctOptionIndex: i }))}
                                style={{ accentColor: 'var(--primary)' }} />
                              <span style={{ fontSize: 12, fontWeight: 700, color: 'var(--primary)' }}>
                                {String.fromCharCode(65 + i)} {form.correctOptionIndex === i ? '✓ Correct' : ''}
                              </span>
                            </div>
                            <input value={opt} placeholder={`Option ${String.fromCharCode(65 + i)} text`}
                              onChange={e => setOption(i, e.target.value)}
                              style={{ ...inp, marginBottom: 6 }} />
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                              <input ref={optImgRefs[i]} type="file" accept="image/*"
                                onChange={e => setOptionImage(i, e.target.files[0] || null)}
                                style={{ fontSize: 11 }} />
                              <ImageThumb file={optionImageFiles[i]} label={`Option ${String.fromCharCode(65 + i)} image`}
                                onRemove={() => { setOptionImage(i, null); if (optImgRefs[i].current) optImgRefs[i].current.value = ''; }} />
                            </div>
                          </div>
                        ))}
                      </div>

                      {/* Meta fields */}
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, marginBottom: 14 }}>
                        <div>
                          <label style={lbl}>Difficulty</label>
                          <select value={form.difficulty} onChange={e => setForm(f => ({ ...f, difficulty: e.target.value }))} style={inp}>
                            <option value="EASY">Easy</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="HARD">Hard</option>
                          </select>
                        </div>
                        <div>
                          <label style={lbl}>Marks</label>
                          <input type="number" min="1" value={form.marks}
                            onChange={e => setForm(f => ({ ...f, marks: parseInt(e.target.value) || 1 }))}
                            style={inp} />
                        </div>
                        <div>
                          <label style={lbl}>Negative Marks</label>
                          <input type="number" min="0" step="0.25" value={form.negativeMarks}
                            onChange={e => setForm(f => ({ ...f, negativeMarks: parseFloat(e.target.value) || 0 }))}
                            style={inp} />
                        </div>
                      </div>

                      {uploadSuccess && (
                        <div style={{ marginBottom: 10, padding: '8px 12px', borderRadius: 5, fontSize: 13, background: '#f0fdf4', color: '#166534', border: '1px solid #bbf7d0' }}>
                          ✓ Question uploaded successfully!
                        </div>
                      )}
                      {error && (
                        <div style={{ marginBottom: 10, padding: '8px 12px', borderRadius: 5, fontSize: 13, background: '#fef2f2', color: '#b91c1c', border: '1px solid #fecaca' }}>
                          {error}
                        </div>
                      )}

                      <button type="submit"
                        style={{ width: '100%', padding: '10px', background: 'var(--primary)', color: '#fff', border: 'none', borderRadius: 6, fontWeight: 700, fontSize: 14, cursor: 'pointer' }}>
                        Upload Question
                      </button>
                    </form>
                  </div>

                  {/* EXCEL UPLOAD */}
                  <div style={{ padding: 20, borderTop: '1px solid var(--border)' }}>
                    <h4 style={{ marginBottom: 6, fontSize: 14, fontWeight: 700 }}>Bulk Upload (Excel / ZIP Bundle)</h4>
                    <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 10 }}>
                      Supported modes: (1) Excel only (.xlsx), (2) Excel + image ZIP, (3) single ZIP bundle containing one Excel and image folders.
                    </p>
                    <input ref={excelInputRef} data-excel="1" type="file" accept=".xlsx,.zip"
                      onChange={e => { setQuestionFile(e.target.files[0] || null); setMsg(''); }}
                      style={{ fontSize: 13, marginBottom: 8, display: 'block' }} />
                    {questionFile && (
                      <div style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 8 }}>
                        Primary file: {questionFile.name} ({(questionFile.size / 1024).toFixed(1)} KB)
                      </div>
                    )}
                    {questionFile && questionFile.name.toLowerCase().endsWith('.xlsx') && (
                      <>
                        <input ref={imageZipInputRef} type="file" accept=".zip"
                          onChange={e => { setImageZipFile(e.target.files[0] || null); setMsg(''); }}
                          style={{ fontSize: 13, marginBottom: 8, display: 'block' }} />
                        <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 8 }}>
                          Optional: image ZIP for Excel image path columns (`question_image`, `option1_image`...`option4_image`, `combined_option_image`).
                        </div>
                      </>
                    )}
                    {imageZipFile && (
                      <div style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 8 }}>
                        Image ZIP: {imageZipFile.name} ({(imageZipFile.size / 1024).toFixed(1)} KB)
                      </div>
                    )}
                    <button onClick={handleUploadQuestions} disabled={!questionFile}
                      style={{ padding: '9px 18px', background: 'var(--primary)', color: '#fff', border: 'none', borderRadius: 5, fontWeight: 600, cursor: questionFile ? 'pointer' : 'not-allowed', opacity: questionFile ? 1 : 0.5 }}>
                      Upload Bulk File
                    </button>
                    {msg && (
                      <div style={{ marginTop: 10, padding: '8px 12px', borderRadius: 5, fontSize: 13, background: msg.startsWith('✓') ? '#f0fdf4' : '#fef2f2', color: msg.startsWith('✓') ? 'var(--success)' : 'var(--danger)', border: '1px solid ' + (msg.startsWith('✓') ? '#bbf7d0' : '#fecaca') }}>
                        {msg}
                      </div>
                    )}
                  </div>
                </div>

                {/* ── QUESTION BANK ── */}
                <div style={{ background: '#fff', borderRadius: 10, boxShadow: 'var(--shadow-sm)' }}>
                  <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)' }}>
                    <h3 style={{ fontSize: 15 }}>Question Bank ({questions.length})</h3>
                  </div>
                  <div>
                    {loading ? (
                      <div style={{ padding: 24, textAlign: 'center' }}><Spinner /></div>
                    ) : questions.length === 0 ? (
                      <div style={{ padding: 24, textAlign: 'center', color: 'var(--text-muted)', fontSize: 13 }}>
                        No questions yet. Upload your first question.
                      </div>
                    ) : (
                      questions.map((q, i) => (
                        <div key={q.id} style={{ padding: '12px 16px', borderBottom: '1px solid var(--border-light)' }}>
                          <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
                            <span style={{ fontWeight: 700, fontSize: 12, color: 'var(--text-muted)', minWidth: 28 }}>Q{i + 1}</span>
                            <div style={{ flex: 1 }}>
                              {q.hasQuestionImage && (
                                <img src={`/questions/${q.id}/image`} alt="Question"
                                  style={{ maxWidth: '100%', maxHeight: 120, objectFit: 'contain', borderRadius: 4, marginBottom: 4, border: '1px solid var(--border)' }} />
                              )}
                              <p style={{ margin: 0, fontSize: 13, lineHeight: 1.5 }}>{q.questionText}</p>
                              <div style={{ marginTop: 6, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                                {q.options && q.options.map((opt, oi) => (
                                  <div key={oi} style={{ fontSize: 11, padding: '4px 8px', borderRadius: 3, background: q.correctOptionIndex === oi ? '#dcfce7' : '#f1f5f9', color: q.correctOptionIndex === oi ? '#166534' : '#475569', fontWeight: q.correctOptionIndex === oi ? 700 : 400, display: 'flex', flexDirection: 'column', gap: 4, minWidth: 90 }}>
                                    <span>{String.fromCharCode(65 + oi)}. {opt}</span>
                                    {q.optionHasImage && q.optionHasImage[oi] && (
                                      <img
                                        src={`/questions/${q.id}/option-image/${oi}`}
                                        alt={`Option ${String.fromCharCode(65 + oi)}`}
                                        style={{ maxWidth: 90, maxHeight: 60, objectFit: 'contain', borderRadius: 4, border: '1px solid var(--border)' }}
                                      />
                                    )}
                                  </div>
                                ))}
                              </div>
                              <div style={{ marginTop: 4, display: 'flex', gap: 6 }}>
                                <span style={{ fontSize: 10, color: 'var(--text-muted)' }}>{q.difficulty}</span>
                                <span style={{ fontSize: 10, color: '#16a34a' }}>+{q.marks}</span>
                                <span style={{ fontSize: 10, color: '#dc2626' }}>-{q.negativeMarks}</span>
                              </div>
                            </div>
                          </div>
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