import React from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const ROLE_LABEL = { ADMIN:"Administrator", TEACHER:"Teacher", STUDENT:"Student" };

function Topbar({ subtitle }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  return (
    <div style={{ background:"var(--primary)", color:"#fff", height:"var(--topbar-height)",
      padding:"0 24px", display:"flex", alignItems:"center", justifyContent:"space-between",
      flexShrink:0, boxShadow:"var(--shadow-md)", zIndex:100 }}>
      <div style={{ display:"flex", alignItems:"center", gap:12 }}>
        <div style={{ width:34, height:34, background:"rgba(255,255,255,0.15)", borderRadius:6,
          display:"flex", alignItems:"center", justifyContent:"center", fontSize:13, fontWeight:700 }}>EP</div>
        <div>
          <div style={{ fontSize:15, fontWeight:700, lineHeight:1.3 }}>Examination Portal</div>
          {subtitle && <div style={{ fontSize:11, opacity:0.75 }}>{subtitle}</div>}
        </div>
      </div>
      <div style={{ display:"flex", alignItems:"center", gap:14 }}>
        <div style={{ textAlign:"right" }}>
          <div style={{ fontSize:13, fontWeight:600 }}>{user?.fullName}</div>
          <div style={{ fontSize:11, opacity:0.75 }}>{ROLE_LABEL[user?.role]}</div>
        </div>
        <button onClick={() => { logout(); navigate("/login"); }}
          style={{ background:"rgba(255,255,255,0.12)", color:"#fff", border:"1px solid rgba(255,255,255,0.25)",
            padding:"5px 14px", borderRadius:5, fontSize:13, cursor:"pointer", fontWeight:500 }}>
          Sign Out
        </button>
      </div>
    </div>
  );
}

export default Topbar;
