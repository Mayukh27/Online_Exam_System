import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getPendingTeachers, approveTeacher, getStats, getSubjects, createSubject, deleteSubject } from '../api/adminApi';
import { createBlueprint, createExam, publishExam, getAllExams, getBlueprints, deleteBlueprint } from '../api/examApi';
import ExamResultsViewer from '../components/ExamResultsViewer';
import Spinner from '../components/Spinner';
import api from '../api/axiosConfig';

const TABS = ['Overview','Teachers','Subjects','Blueprints','Exams','Results'];

export default function AdminDashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [tab,    setTab]    = useState('Overview');
  const [stats,  setStats]  = useState({});
  const [teachers, setTeachers] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [exams,    setExams]    = useState([]);
  const [blueprints, setBlueprints] = useState([]);
  const [selectedBlueprint, setSelectedBlueprint] = useState(null);
  const [loading, setLoading] = useState(false);
  const [msg,     setMsg]     = useState({ text:'', ok:true });
  const [subjectForm, setSubjectForm] = useState({ name:'', code:'', description:'' });
  const [examForm,    setExamForm]    = useState({ title:'', description:'', blueprintId:'', scheduledStart:'', scheduledEnd:'', durationMinutes:60 });
  const [blueprintForm, setBlueprintForm] = useState({ name:'', description:'', durationMinutes:60, totalMarks:10, entries:[] });
  const showSectionName = blueprintForm.entries.length > 1;
  
  const flash = (text, ok=true) => { setMsg({text,ok}); setTimeout(()=>setMsg({text:'',ok:true}),4000); };
  useEffect(() => {
  const handleBack = async () => {
    try {
      await api.post("/auth/logout");
    } catch (e) {
      console.warn("Logout failed");
    }

    localStorage.removeItem("examportal_user");
    localStorage.removeItem("exam_active");

    window.location.href = "/login";
  };

  window.history.pushState(null, "", window.location.href);
  window.addEventListener("popstate", handleBack);

  return () => {
    window.removeEventListener("popstate", handleBack);
  };
}, []);
  useEffect(() => {
    getStats().then(r=>setStats(r.data.data||{})).catch(()=>{});
    getSubjects().then(r=>setSubjects(r.data.data||[])).catch(()=>{});
  }, []);

  useEffect(() => {
    if (tab==='Teachers') { setLoading(true); getPendingTeachers().then(r=>setTeachers(r.data.data||[])).catch(()=>{}).finally(()=>setLoading(false)); }
    if (tab==='Exams')    { setLoading(true); Promise.all([getAllExams(), getBlueprints()]).then(([er,br])=>{ setExams(er.data.data||[]); setBlueprints(br.data.data||[]); }).catch(()=>{}).finally(()=>setLoading(false)); }
    if (tab==='Blueprints') { loadBlueprints(); }
  }, [tab]);

  const loadBlueprints = () => getBlueprints().then(r=>setBlueprints(r.data.data||[])).catch(()=>{});

  const handleApprove = async (id) => {
    try { await approveTeacher(id); setTeachers(p=>p.filter(t=>t.id!==id)); flash('Teacher approved.'); }
    catch(e) { flash(e.response?.data?.message||'Failed to approve.',false); }
  };

  const handleCreateSubject = async (e) => {
    e.preventDefault();
    try { const r=await createSubject(subjectForm); setSubjects(p=>[...p,r.data.data]); setSubjectForm({name:'',code:'',description:''}); flash('Subject created.'); }
    catch(e) { flash(e.response?.data?.message||'Failed to create subject.',false); }
  };

  const handleDeleteSubject = async (id, name) => {
    if (!window.confirm('Delete subject "'+name+'" and ALL its questions? This cannot be undone.')) return;
    try { await deleteSubject(id); setSubjects(p=>p.filter(s=>s.id!==id)); flash('Subject deleted.'); }
    catch(e) { flash(e.response?.data?.message||'Failed to delete subject.',false); }
  };

  const handleCreateBlueprint = async (e) => {
    e.preventDefault();
    try { const r=await createBlueprint(blueprintForm); setBlueprints(p=>[...p,r.data.data]); setBlueprintForm({name:'',description:'',durationMinutes:60,totalMarks:10,entries:[]}); flash('Blueprint created.'); }
    catch(e) { flash(e.response?.data?.message||'Failed to create blueprint.',false); }
  };

  const handleDeleteBlueprint = async (id) => {
    if (!window.confirm('Delete this blueprint?')) return;
    try { await deleteBlueprint(id); setBlueprints(p=>p.filter(b=>b.id!==id)); setSelectedBlueprint(null); flash('Blueprint deleted.'); }
    catch(e) { flash(e.response?.data?.message||'Cannot delete blueprint in use.',false); }
  };

  const handleCreateExam = async (e) => {
    e.preventDefault();
    try { const r=await createExam(examForm); setExams(p=>[...p,r.data.data]); setExamForm({title:'',description:'',blueprintId:'',scheduledStart:'',scheduledEnd:'',durationMinutes:60}); flash('Exam created as DRAFT.'); }
    catch(e) { flash(e.response?.data?.message||'Failed to create exam.',false); }
  };

  const handlePublish = async (id) => {
    try { await publishExam(id); setExams(p=>p.map(e=>e.id===id?{...e,status:'PUBLISHED'}:e)); flash('Exam published.'); }
    catch(e) { flash(e.response?.data?.message||'Publish failed. Ensure enough questions exist.',false); }
  };

  const inp = { width:'100%', padding:'8px 10px', border:'1px solid var(--border)', borderRadius:5, fontSize:13 };
  const lbl = { fontSize:12, fontWeight:700, color:'var(--text-secondary)', display:'block', marginBottom:5 };

  const statusBadge = (s) => {
    const map = { DRAFT:['#f5f3ff','#6d28d9'], PUBLISHED:['#f0fdf4','#166534'], COMPLETED:['#f1f5f9','#64748b'], CANCELLED:['#fef2f2','#991b1b'] };
    const [bg,color] = map[s]||['#f1f5f9','#64748b'];
    return <span style={{padding:'2px 8px',borderRadius:4,fontSize:11,fontWeight:700,background:bg,color}}>{s}</span>;
  };

  const tabIcon = { Overview:'📊', Teachers:'👨‍🏫', Subjects:'📚', Blueprints:'🗺', Exams:'📋', Results:'🏆' };
  
  const handleLogout = async () => {
    try {
      await api.post("/auth/logout");
    } catch (e) {
        console.warn("Logout API failed");
      }

   localStorage.removeItem("exam_active");
   logout();
   navigate('/login');
 };

  return (
    <div style={{minHeight:'100vh',background:'var(--bg)',display:'flex',flexDirection:'column'}}>
      {/* Navbar */}
      <div style={{background:'#fff',borderBottom:'1px solid var(--border)',height:56,display:'flex',alignItems:'center',padding:'0 24px',justifyContent:'space-between',boxShadow:'var(--shadow-sm)',flexShrink:0}}>
        <div style={{display:'flex',alignItems:'center',gap:10}}>
          <span style={{fontSize:20}}>📝</span>
          <span style={{fontWeight:700,fontSize:17,color:'var(--primary)'}}>ExamPortal</span>
          <span style={{marginLeft:8,padding:'2px 8px',background:'#fef3c7',color:'#92400e',borderRadius:4,fontSize:11,fontWeight:700}}>ADMIN</span>
        </div>
        <div style={{display:'flex',alignItems:'center',gap:16}}>
          <span style={{fontSize:13,color:'var(--text-secondary)'}}>Welcome, <strong>{user?.fullName}</strong></span>
          <button onClick={()=>{handleLogout();}} style={{padding:'6px 14px',border:'1px solid var(--border)',borderRadius:6,background:'#fff',color:'var(--text-secondary)',cursor:'pointer',fontSize:13}}>Sign out</button>
        </div>
      </div>

      <div style={{flex:1,display:'flex',overflow:'hidden'}}>
        {/* Sidebar */}
        <div style={{width:200,background:'#fff',borderRight:'1px solid var(--border)',flexShrink:0,overflowY:'auto'}}>
          {TABS.map(t=>(
            <button key={t} onClick={()=>{setTab(t);setMsg({text:'',ok:true});}}
              style={{width:'100%',textAlign:'left',padding:'12px 20px',border:'none',borderBottom:'1px solid var(--border-light)',
                background:tab===t?'var(--primary-light)':'transparent',
                color:tab===t?'var(--primary)':'var(--text-primary)',
                fontWeight:tab===t?700:400,cursor:'pointer',fontSize:14}}>
              {tabIcon[t]} {t}
            </button>
          ))}
        </div>

        {/* Main */}
        <div style={{flex:1,overflowY:'auto',padding:28}}>
          {msg.text && (
            <div style={{background:msg.ok?'#f0fdf4':'#fef2f2',border:'1px solid '+(msg.ok?'#bbf7d0':'#fecaca'),
              color:msg.ok?'var(--success)':'var(--danger)',padding:'10px 16px',borderRadius:7,marginBottom:20,fontSize:13,fontWeight:600}}>
              {msg.text}
            </div>
          )}

          {/* OVERVIEW */}
          {tab==='Overview' && (
            <div>
              <h2 style={{fontSize:22,fontWeight:700,marginBottom:20}}>Platform Overview</h2>
              <div style={{display:'grid',gridTemplateColumns:'repeat(3,1fr)',gap:20}}>
                {[
                  {label:'Total Students',val:stats.totalStudents??'—',icon:'🎓',color:'var(--primary)'},
                  {label:'Total Teachers',val:stats.totalTeachers??'—',icon:'👨‍🏫',color:'var(--success)'},
                  {label:'Subjects',val:subjects.length,icon:'📚',color:'var(--warning)'},
                ].map(c=>(
                  <div key={c.label} style={{background:'#fff',borderRadius:12,boxShadow:'var(--shadow-sm)',padding:24}}>
                    <div style={{display:'flex',alignItems:'center',gap:12}}>
                      <span style={{fontSize:28}}>{c.icon}</span>
                      <div>
                        <div style={{fontSize:28,fontWeight:800,color:c.color}}>{c.val}</div>
                        <div style={{fontSize:13,color:'var(--text-muted)'}}>{c.label}</div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TEACHERS */}
          {tab==='Teachers' && (
            <div>
              <h2 style={{fontSize:22,fontWeight:700,marginBottom:20}}>Pending Teacher Approvals</h2>
              {loading ? <div style={{display:'flex',justifyContent:'center',padding:40}}><Spinner size={36}/></div>
              : teachers.length===0 ? (
                <div style={{background:'#fff',borderRadius:12,padding:40,textAlign:'center',boxShadow:'var(--shadow-sm)'}}>
                  <div style={{fontSize:40,marginBottom:12}}>✅</div>
                  <p style={{color:'var(--text-muted)'}}>No pending teacher approvals.</p>
                </div>
              ) : (
                <div style={{background:'#fff',borderRadius:12,boxShadow:'var(--shadow-sm)',overflow:'hidden'}}>
                  <table style={{width:'100%',borderCollapse:'collapse'}}>
                    <thead>
                      <tr style={{background:'var(--bg-panel)',borderBottom:'1px solid var(--border)'}}>
                        {['Name','Email','Registered','Action'].map(h=>(
                          <th key={h} style={{padding:'12px 16px',textAlign:'left',fontSize:12,fontWeight:700,color:'var(--text-secondary)'}}>{h}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {teachers.map(t=>(
                        <tr key={t.id} style={{borderBottom:'1px solid var(--border-light)'}}>
                          <td style={{padding:'12px 16px',fontWeight:600}}>{t.fullName}</td>
                          <td style={{padding:'12px 16px',color:'var(--text-muted)'}}>{t.email}</td>
                          <td style={{padding:'12px 16px',color:'var(--text-muted)'}}>{t.createdAt?new Date(t.createdAt).toLocaleDateString():'-'}</td>
                          <td style={{padding:'12px 16px'}}>
                            <button onClick={()=>handleApprove(t.id)}
                              style={{padding:'6px 16px',background:'var(--success)',color:'#fff',border:'none',borderRadius:5,fontWeight:600,cursor:'pointer',fontSize:12}}>
                              Approve
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* SUBJECTS */}
          {tab==='Subjects' && (
            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:24}}>
              <div style={{background:'#fff',borderRadius:10,boxShadow:'var(--shadow-sm)',overflow:'hidden'}}>
                <div style={{padding:'14px 20px',background:'var(--bg-panel)',borderBottom:'1px solid var(--border)'}}>
                  <h3 style={{fontSize:15,fontWeight:700}}>Create New Subject</h3>
                  <p style={{fontSize:12,color:'var(--text-muted)',marginTop:2}}>Subject Code = Subject ID (unique external reference)</p>
                </div>
                <form onSubmit={handleCreateSubject} style={{padding:20,display:'flex',flexDirection:'column',gap:14}}>
                  <div><label style={lbl}>Subject Name</label><input value={subjectForm.name} onChange={e=>setSubjectForm(f=>({...f,name:e.target.value}))} required placeholder="Engineering Mathematics" style={inp}/></div>
                  <div><label style={lbl}>Subject Code</label><input value={subjectForm.code} onChange={e=>setSubjectForm(f=>({...f,code:e.target.value}))} required placeholder="MATH101" style={inp}/></div>
                  <div><label style={lbl}>Description</label><textarea value={subjectForm.description} onChange={e=>setSubjectForm(f=>({...f,description:e.target.value}))} rows={3} style={{...inp,resize:'vertical',fontFamily:'inherit'}}/></div>
                  <button type="submit" style={{padding:'10px',background:'var(--primary)',color:'#fff',border:'none',borderRadius:5,fontWeight:600,cursor:'pointer'}}>Create Subject</button>
                </form>
              </div>
              <div style={{background:'#fff',borderRadius:10,boxShadow:'var(--shadow-sm)',overflow:'hidden'}}>
                <div style={{padding:'14px 20px',background:'var(--bg-panel)',borderBottom:'1px solid var(--border)'}}>
                  <h3 style={{fontSize:15,fontWeight:700}}>All Subjects ({subjects.length})</h3>
                </div>
                <div style={{maxHeight:420,overflowY:'auto'}}>
                  {subjects.map(s=>(
                    <div key={s.id} style={{padding:'12px 20px',borderBottom:'1px solid var(--border-light)',display:'flex',justifyContent:'space-between',alignItems:'center'}}>
                      <div>
                        <div style={{fontWeight:600,fontSize:14}}>{s.name}</div>
                        <div style={{fontSize:12,color:'var(--text-muted)'}}>Code (ID): <strong>{s.code}</strong></div>
                      </div>
                      <button onClick={()=>handleDeleteSubject(s.id,s.name)}
                        style={{padding:'5px 12px',background:'#fef2f2',color:'var(--danger)',border:'1px solid #fecaca',borderRadius:5,cursor:'pointer',fontSize:12,fontWeight:600}}>
                        🗑 Delete
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* BLUEPRINTS */}
          {tab==='Blueprints' && (
            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:24}}>

              {/* CREATE BLUEPRINT */}
              <div style={{background:'#fff',padding:24,borderRadius:10,boxShadow:'var(--shadow-sm)'}}>
                <h3 style={{fontSize:15,fontWeight:700,marginBottom:18}}>Create Blueprint</h3>
                <form onSubmit={handleCreateBlueprint} style={{display:'flex',flexDirection:'column',gap:12}}>
                  <div><label style={lbl}>Blueprint Name</label>
                    <input value={blueprintForm.name} onChange={e=>setBlueprintForm(f=>({...f,name:e.target.value}))} style={inp} required placeholder="e.g. GATE 2025"/>
                  </div>
                  <div><label style={lbl}>Description</label>
                    <input value={blueprintForm.description} onChange={e=>setBlueprintForm(f=>({...f,description:e.target.value}))} style={inp} placeholder="Optional"/>
                  </div>
                  <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:10}}>
                    <div><label style={lbl}>Duration (minutes)</label>
                      <input type="number" min={5} value={blueprintForm.durationMinutes} onChange={e=>setBlueprintForm(f=>({...f,durationMinutes:Number(e.target.value)}))} style={inp}/>
                    </div>
                    <div><label style={lbl}>Total Marks (auto)</label>
                      <input readOnly value={blueprintForm.entries.reduce((s,e)=>s+(e.questionCount||0)*(e.marksPerQuestion||0),0)} style={{...inp,background:'#f9fafb',cursor:'default'}}/>
                    </div>
                  </div>

                  {/* ENTRIES */}
                  <div>
                    <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:8}}>
                      <label style={{...lbl,marginBottom:0}}>Subject Entries</label>
                      <button type="button"
                        onClick={()=>setBlueprintForm(f=>({...f,entries:[...f.entries,{subjectId:'',sectionName:'',questionCount:10,marksPerQuestion:1,negativeMarks:0.25}]}))}
                        style={{padding:'4px 12px',background:'var(--primary-light)',color:'var(--primary)',border:'1px solid var(--primary)',borderRadius:5,fontWeight:700,cursor:'pointer',fontSize:12}}>
                        + Add Subject
                      </button>
                    </div>

                    {blueprintForm.entries.length===0 && (
                      <div style={{padding:'14px',background:'#f9fafb',border:'1px dashed var(--border)',borderRadius:6,textAlign:'center',fontSize:13,color:'var(--text-muted)'}}>
                        Click "+ Add Subject" to add subjects to this blueprint
                      </div>
                    )}

                    {blueprintForm.entries.map((entry,i)=>(
                      <div key={i} style={{border:'1px solid var(--border)',borderRadius:7,padding:12,marginBottom:8,background:'#fafafa'}}>
                        <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:8}}>
                          <span style={{fontSize:12,fontWeight:700,color:'var(--primary)'}}>Entry {i+1}</span>
                          <button type="button" onClick={()=>setBlueprintForm(f=>({...f,entries:f.entries.filter((_,j)=>j!==i)}))}
                            style={{border:'none',background:'transparent',color:'var(--danger)',cursor:'pointer',fontSize:16}}>✕</button>
                        </div>

                        <div style={{marginBottom:8}}>
                          <label style={lbl}>Subject</label>
                          <select value={entry.subjectId}
                            onChange={e=>setBlueprintForm(f=>({...f,entries:f.entries.map((en,j)=>j===i?{...en,subjectId:Number(e.target.value)}:en)}))}
                            required style={inp}>
                            <option value="">Select Subject</option>
                            {subjects.map(s=><option key={s.id} value={s.id}>{s.name} ({s.code})</option>)}
                          </select>
                        </div>
                        {showSectionName && (
                        <div style={{marginBottom:8}}>
                          <label style={lbl}>Section Name </label>
                          <input value={entry.sectionName||''} onChange={e=>setBlueprintForm(f=>({...f,entries:f.entries.map((en,j)=>j===i?{...en,sectionName:e.target.value}:en)}))}
                            placeholder="e.g. Section A – MCQ  (leave blank if not needed)" style={inp}/>
                        </div>
                        )}  

                        <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:8}}>
                          <div><label style={lbl}>Questions</label>
                            <input type="number" min={1} value={entry.questionCount}
                              onChange={e=>setBlueprintForm(f=>({...f,entries:f.entries.map((en,j)=>j===i?{...en,questionCount:Number(e.target.value)}:en)}))} style={inp}/>
                          </div>
                          <div><label style={lbl}>Marks (+)</label>
                            <input type="number" min={0} step={0.25} value={entry.marksPerQuestion}
                              onChange={e=>setBlueprintForm(f=>({...f,entries:f.entries.map((en,j)=>j===i?{...en,marksPerQuestion:Number(e.target.value)}:en)}))} style={inp}/>
                          </div>
                          <div><label style={lbl}>Negative (-)</label>
                            <input type="number" min={0} step={0.25} value={entry.negativeMarks||0.25}
                              onChange={e=>setBlueprintForm(f=>({...f,entries:f.entries.map((en,j)=>j===i?{...en,negativeMarks:Number(e.target.value)}:en)}))} style={inp}/>
                          </div>
                        </div>

                        {entry.questionCount>0 && entry.marksPerQuestion>0 && (
                          <div style={{marginTop:6,fontSize:12,color:'var(--text-muted)',background:'#f0f9ff',padding:'4px 10px',borderRadius:4}}>
                            {entry.questionCount} × {entry.marksPerQuestion} = <b>{entry.questionCount*entry.marksPerQuestion}</b> marks
                          </div>
                        )}
                      </div>
                    ))}

                    {blueprintForm.entries.length>0 && (
                      <div style={{padding:'8px 12px',background:'var(--primary-light)',borderRadius:6,fontSize:13,fontWeight:700,color:'var(--primary)',display:'flex',justifyContent:'space-between'}}>
                        <span>Total Questions: {blueprintForm.entries.reduce((s,e)=>s+(e.questionCount||0),0)}</span>
                        <span>Total Marks: {blueprintForm.entries.reduce((s,e)=>s+(e.questionCount||0)*(e.marksPerQuestion||0),0)}</span>
                      </div>
                    )}
                  </div>

                  <button type="submit"
                    disabled={blueprintForm.entries.length===0||blueprintForm.entries.some(e=>!e.subjectId)}
                    style={{padding:'11px',background:'var(--primary)',color:'#fff',border:'none',borderRadius:6,fontWeight:700,cursor:'pointer',
                      opacity:blueprintForm.entries.length===0||blueprintForm.entries.some(e=>!e.subjectId)?0.5:1}}>
                    Create Blueprint
                  </button>
                </form>
              </div>

              {/* BLUEPRINT LIST */}
              <div style={{background:'#fff',borderRadius:10,boxShadow:'var(--shadow-sm)',overflow:'hidden'}}>
                {!selectedBlueprint ? (
                  <>
                    <div style={{padding:'14px 20px',background:'var(--bg-panel)',borderBottom:'1px solid var(--border)'}}>
                      <h3 style={{fontSize:15,fontWeight:700}}>Blueprints ({blueprints.length})</h3>
                    </div>
                    <div style={{maxHeight:600,overflowY:'auto'}}>
                      {blueprints.length===0 && (
                        <div style={{padding:40,textAlign:'center',color:'var(--text-muted)'}}>
                          <div style={{fontSize:32,marginBottom:10}}>🗺</div>
                          No blueprints yet.
                        </div>
                      )}
                      {blueprints.map(b=>(
                        <div key={b.id} style={{padding:'14px 20px',borderBottom:'1px solid var(--border-light)',display:'flex',justifyContent:'space-between',alignItems:'center'}}>
                          <div>
                            <div style={{fontWeight:600,fontSize:14}}>{b.name}</div>
                            <div style={{fontSize:12,color:'var(--text-muted)',marginTop:2}}>
                              {b.durationMinutes} min | {b.totalMarks} marks | {b.entries?.length||0} subject(s)
                            </div>
                          </div>
                          <div style={{display:'flex',gap:6}}>
                            <button onClick={()=>setSelectedBlueprint(b)}
                              style={{padding:'5px 12px',border:'1px solid var(--border)',background:'#fff',borderRadius:5,cursor:'pointer',fontSize:12,fontWeight:600}}>
                              👁 View
                            </button>
                            <button onClick={()=>handleDeleteBlueprint(b.id)}
                              style={{padding:'5px 12px',background:'#fef2f2',color:'var(--danger)',border:'1px solid #fecaca',borderRadius:5,cursor:'pointer',fontSize:12,fontWeight:600}}>
                              🗑 Delete
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </>
                ) : (
                  <>
                    <div style={{padding:'14px 20px',background:'var(--bg-panel)',borderBottom:'1px solid var(--border)',display:'flex',justifyContent:'space-between',alignItems:'center'}}>
                      <h3 style={{fontSize:15,fontWeight:700}}>{selectedBlueprint.name}</h3>
                      <button onClick={()=>setSelectedBlueprint(null)} style={{border:'none',background:'transparent',fontSize:18,cursor:'pointer',color:'var(--text-muted)'}}>✖</button>
                    </div>
                    <div style={{padding:20}}>
                      {selectedBlueprint.description && <p style={{color:'var(--text-muted)',marginBottom:12,fontSize:13}}>{selectedBlueprint.description}</p>}
                      <div style={{display:'flex',gap:20,marginBottom:16,fontSize:13}}>
                        <span>⏱ <b>{selectedBlueprint.durationMinutes} min</b></span>
                        <span>🏆 <b>{selectedBlueprint.totalMarks} marks</b></span>
                        <span>📚 <b>{selectedBlueprint.entries?.length||0} subject(s)</b></span>
                      </div>
                      <table style={{width:'100%',borderCollapse:'collapse',fontSize:13}}>
                        <thead>
                          <tr style={{background:'var(--bg-panel)',borderBottom:'1px solid var(--border)'}}>
                            {['Subject','Section Name','Questions','Marks (+)','Negative (-)','Subtotal'].map(h=>(
                              <th key={h} style={{padding:'8px 10px',textAlign:'left',fontWeight:700,color:'var(--text-secondary)',fontSize:11}}>{h}</th>
                            ))}
                          </tr>
                        </thead>
                        <tbody>
                          {selectedBlueprint.entries?.map((e,i)=>{
                            const subj=subjects.find(s=>s.id===e.subjectId);
                            return (
                              <tr key={i} style={{borderBottom:'1px solid var(--border-light)',background:i%2?'#fafafa':'#fff'}}>
                                <td style={{padding:'8px 10px',fontWeight:600}}>{subj?.name||('Subject '+e.subjectId)}</td>
                                <td style={{padding:'8px 10px',color:'var(--text-muted)',fontStyle:e.sectionName?'normal':'italic'}}>{e.sectionName||'—'}</td>
                                <td style={{padding:'8px 10px'}}>{e.questionCount}</td>
                                <td style={{padding:'8px 10px',color:'var(--success)',fontWeight:600}}>+{e.marksPerQuestion}</td>
                                <td style={{padding:'8px 10px',color:'var(--danger)',fontWeight:600}}>-{e.negativeMarks||0.25}</td>
                                <td style={{padding:'8px 10px',fontWeight:700}}>{(e.questionCount||0)*(e.marksPerQuestion||0)}</td>
                              </tr>
                            );
                          })}
                        </tbody>
                        <tfoot>
                          <tr style={{background:'var(--primary-light)',borderTop:'2px solid var(--primary)'}}>
                            <td colSpan={2} style={{padding:'9px 10px',fontWeight:700,color:'var(--primary)'}}>Total</td>
                            <td style={{padding:'9px 10px',fontWeight:700}}>{selectedBlueprint.entries?.reduce((s,e)=>s+(e.questionCount||0),0)}</td>
                            <td colSpan={2}></td>
                            <td style={{padding:'9px 10px',fontWeight:800,color:'var(--primary)'}}>{selectedBlueprint.totalMarks}</td>
                          </tr>
                        </tfoot>
                      </table>
                    </div>
                  </>
                )}
              </div>

            </div>
          )}

          {/* EXAMS */}
          {tab==='Exams' && (
            <div>
              <h2 style={{fontSize:22,fontWeight:700,marginBottom:20}}>Exam Management</h2>
              <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:24}}>
                <div style={{background:'#fff',borderRadius:10,boxShadow:'var(--shadow-sm)',overflow:'hidden'}}>
                  <div style={{padding:'14px 20px',background:'var(--bg-panel)',borderBottom:'1px solid var(--border)'}}><h3 style={{fontSize:15,fontWeight:700}}>Create New Exam</h3></div>
                  <form onSubmit={handleCreateExam} style={{padding:20,display:'flex',flexDirection:'column',gap:12}}>
                    <div><label style={lbl}>Title</label><input value={examForm.title} onChange={e=>setExamForm(f=>({...f,title:e.target.value}))} required style={inp}/></div>
                    <div><label style={lbl}>Description</label><input value={examForm.description} onChange={e=>setExamForm(f=>({...f,description:e.target.value}))} style={inp}/></div>
                    <div><label style={lbl}>Blueprint</label>
                      <select value={examForm.blueprintId} onChange={e=>setExamForm(f=>({...f,blueprintId:Number(e.target.value)}))} required style={inp}>
                        <option value="">Select Blueprint</option>
                        {blueprints.map(b=><option key={b.id} value={b.id}>{b.name}</option>)}
                      </select>
                    </div>
                    <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:10}}>
                      <div><label style={lbl}>Start Time</label><input type="datetime-local" value={examForm.scheduledStart} onChange={e=>setExamForm(f=>({...f,scheduledStart:e.target.value}))} required style={inp}/></div>
                      <div><label style={lbl}>End Time</label><input type="datetime-local" value={examForm.scheduledEnd} onChange={e=>setExamForm(f=>({...f,scheduledEnd:e.target.value}))} required style={inp}/></div>
                    </div>
                    <div><label style={lbl}>Duration (minutes)</label><input type="number" value={examForm.durationMinutes} min={10} disabled={!!examForm.blueprintId} style={inp}/></div>
                    <button type="submit" style={{padding:'10px',background:'var(--primary)',color:'#fff',border:'none',borderRadius:5,fontWeight:600,cursor:'pointer'}}>Create Exam</button>
                  </form>
                </div>
                <div style={{background:'#fff',borderRadius:10,boxShadow:'var(--shadow-sm)',overflow:'hidden'}}>
                  <div style={{padding:'14px 20px',background:'var(--bg-panel)',borderBottom:'1px solid var(--border)'}}><h3 style={{fontSize:15,fontWeight:700}}>All Exams</h3></div>
                  <div style={{maxHeight:420,overflowY:'auto'}}>
                    {loading?<div style={{padding:30,display:'flex',justifyContent:'center'}}><Spinner/></div>
                    :exams.length===0?<div style={{padding:30,textAlign:'center',color:'var(--text-muted)'}}>No exams created yet.</div>
                    :exams.map(e=>(
                      <div key={e.id} style={{padding:'14px 20px',borderBottom:'1px solid var(--border-light)'}}>
                        <div style={{display:'flex',justifyContent:'space-between',alignItems:'flex-start',marginBottom:6}}>
                          <div style={{fontWeight:600,fontSize:14}}>{e.title}</div>
                          {statusBadge(e.status)}
                        </div>
                        <div style={{fontSize:12,color:'var(--text-muted)',marginBottom:10}}>
                          {e.durationMinutes} min | {e.scheduledStart?new Date(e.scheduledStart).toLocaleString():'-'}
                        </div>
                        {e.status==='DRAFT'&&(
                          <button onClick={()=>handlePublish(e.id)}
                            style={{padding:'5px 14px',background:'var(--success)',color:'#fff',border:'none',borderRadius:4,fontWeight:600,cursor:'pointer',fontSize:12}}>
                            Publish Exam
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* RESULTS */}
          {tab==='Results' && <ExamResultsViewer />}
        </div>
      </div>
    </div>
  );
}