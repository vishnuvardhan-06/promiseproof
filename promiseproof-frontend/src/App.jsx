
import {
    Navigate,
    Route,
    Routes,
    useLocation
} from "react-router-dom";

import Navbar from "./components/Navbar";
import HomePage from "./pages/HomePage";
import PartyPage from "./pages/PartyPage";
import ElectionPage from "./pages/ElectionPage";
import Login from "./pages/Login";
import Register from "./pages/Register";
import DashboardPage from "./pages/DashboardPage";
import PromiseDetails from "./pages/PromiseDetails";
import ReviewQueuePage from "./pages/ReviewQueuePage";
import AdminPage from "./pages/AdminPage";
import { useAuth } from "./auth/useAuth";

import "./App.css";
import "./styles/dashboard.css";

const ROLE_PATHS = {
    citizen: "CITIZEN",
    researcher: "RESEARCHER",
    reviewer: "REVIEWER",
    admin: "ADMIN"
};

function LoadingScreen() {
    return <div className="auth-loading">Checking your session...</div>;
}

function WelcomePage() {
    const { user } = useAuth();

    if (user) {
        return <Navigate to="/dashboard" replace />;
    }

    return (
        <div className="welcome-page">
            <nav className="welcome-nav">
                <div className="welcome-brand">
                    <span className="brand-mark">P</span>
                    PromiseProof
                </div>
                <a href="/login">Sign in</a>
            </nav>

            <section className="welcome-hero">
                <div className="welcome-copy">
                    <span className="welcome-label">
                        PUBLIC PROMISE ACCOUNTABILITY
                    </span>

                    <h1>
                        Every public promise.
                        <span> Backed by proof.</span>
                    </h1>

                    <p>
                        PromiseProof helps people track public commitments
                        through original manifestos, official documents,
                        verified evidence and independent review.
                    </p>

                    <div className="welcome-actions">
                        <a className="welcome-primary" href="/login">
                            Sign in to continue
                        </a>
                        <a className="welcome-secondary" href="/register">
                            Create citizen account
                        </a>
                    </div>

                    <p className="welcome-note">
                        Independent. Evidence-based. Politically neutral.
                    </p>
                </div>

                <div className="welcome-visual">
                    <div className="visual-topline">
                        <span>ACCOUNTABILITY WORKFLOW</span>
                        <span className="live-dot">● SYSTEM</span>
                    </div>

                    <div className="visual-step">
                        <span className="visual-number">01</span>
                        <div>
                            <strong>Original commitment</strong>
                            <small>Manifesto and official source</small>
                        </div>
                    </div>

                    <div className="visual-line" />

                    <div className="visual-step">
                        <span className="visual-number">02</span>
                        <div>
                            <strong>Supporting evidence</strong>
                            <small>Documents and source verification</small>
                        </div>
                    </div>

                    <div className="visual-line" />

                    <div className="visual-step">
                        <span className="visual-number">03</span>
                        <div>
                            <strong>Independent review</strong>
                            <small>Evidence and status decisions</small>
                        </div>
                    </div>

                    <div className="visual-line" />

                    <div className="visual-step">
                        <span className="visual-number">04</span>
                        <div>
                            <strong>Public accountability record</strong>
                            <small>Traceable decisions and history</small>
                        </div>
                    </div>
                </div>
            </section>

            <section className="role-entry">
                <div>
                    <span className="welcome-label">ACCESS PORTAL</span>
                    <h2>Choose your workspace</h2>
                    <p>Sign in using the account assigned to your role.</p>
                </div>

                <div className="role-entry-grid">
                    {Object.entries({
                        citizen: ["Citizen", "Submit evidence and follow contributions"],
                        researcher: ["Researcher", "Record promises and source documents"],
                        reviewer: ["Reviewer", "Review evidence and status proposals"],
                        admin: ["Administrator", "Manage accounts and platform records"]
                    }).map(([path, details]) => (
                        <a
                            className="role-entry-card"
                            href={`/login/${path}`}
                            key={path}
                        >
                            <strong>{details[0]}</strong>
                            <span>{details[1]}</span>
                            <b>Continue →</b>
                        </a>
                    ))}
                </div>
            </section>

            <footer className="welcome-footer">
                PromiseProof · Evidence before conclusions.
            </footer>
        </div>
    );
}

function RequireAuth({ children }) {
    const { user, loading } = useAuth();
    const location = useLocation();

    if (loading) return <LoadingScreen />;

    if (!user) {
        return (
            <Navigate
                to="/login"
                replace
                state={{ from: location.pathname }}
            />
        );
    }

    return children;
}

function RequireRole({ roles, children }) {
    const { user, loading } = useAuth();

    if (loading) return <LoadingScreen />;

    if (!user) return <Navigate to="/login" replace />;

    if (!roles.includes(user.role)) {
        return <Navigate to="/dashboard" replace />;
    }

    return children;
}

function RoleLogin() {
    const { user, loading } = useAuth();
    const location = useLocation();

    const roleKey = location.pathname.split("/").pop();
    const expectedRole = ROLE_PATHS[roleKey];

    if (loading) return <LoadingScreen />;

    if (user) return <Navigate to="/dashboard" replace />;

    if (!expectedRole) return <Navigate to="/login" replace />;

    return <Login expectedRole={expectedRole} />;
}

function App() {
    const { user, loading } = useAuth();

    if (loading) return <LoadingScreen />;

    const isAdmin = user?.role === "ADMIN";

    return (
        <div className="app">
            {user && <Navbar />}

            <main className={user ? "main-container" : "guest-container"}>
                <Routes>
                    <Route path="/" element={<WelcomePage />} />

                    <Route path="/login" element={<Navigate to="/login/citizen" replace />} />

                    <Route path="/login/:role" element={<RoleLogin />} />

                    <Route
                        path="/register"
                        element={user ? <Navigate to="/dashboard" replace /> : <Register />}
                    />

                    <Route
                        path="/dashboard"
                        element={
                            <RequireAuth>
                                <DashboardPage />
                            </RequireAuth>
                        }
                    />

                    <Route
                        path="/promises"
                        element={
                            <RequireAuth>
                                <HomePage />
                            </RequireAuth>
                        }
                    />

                    <Route
                        path="/promises/:id"
                        element={
                            <RequireAuth>
                                <PromiseDetails />
                            </RequireAuth>
                        }
                    />

                    <Route
                        path="/parties"
                        element={
                            <RequireRole roles={["ADMIN", "RESEARCHER", "REVIEWER", "CITIZEN"]}>
                                <PartyPage canManage={isAdmin} />
                            </RequireRole>
                        }
                    />

                    <Route
                        path="/elections"
                        element={
                            <RequireAuth>
                                <ElectionPage canManage={isAdmin} />
                            </RequireAuth>
                        }
                    />

                    <Route
                        path="/review"
                        element={
                            <RequireRole roles={["REVIEWER", "ADMIN"]}>
                                <ReviewQueuePage />
                            </RequireRole>
                        }
                    />

                    <Route
                        path="/admin"
                        element={
                            <RequireRole roles={["ADMIN"]}>
                                <AdminPage />
                            </RequireRole>
                        }
                    />

                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </main>
        </div>
    );
}

export default App;
