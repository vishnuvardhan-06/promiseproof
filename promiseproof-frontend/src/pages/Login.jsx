
import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import { useAuth } from "../auth/useAuth";

const ROLE_DETAILS = {
    CITIZEN: {
        title: "Citizen sign in",
        description: "Access your evidence submissions and notifications.",
        icon: "👤"
    },
    RESEARCHER: {
        title: "Researcher sign in",
        description: "Manage promise records, sources and research submissions.",
        icon: "🔎"
    },
    REVIEWER: {
        title: "Reviewer sign in",
        description: "Review evidence and independently assess proposals.",
        icon: "✓"
    },
    ADMIN: {
        title: "Administrator sign in",
        description: "Access platform administration and audit controls.",
        icon: "⚙"
    }
};

function Login({ expectedRole = "CITIZEN" }) {
    const { login, logout } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const details = ROLE_DETAILS[expectedRole];

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    async function handleSubmit(event) {
        event.preventDefault();

        try {
            setSubmitting(true);
            setError("");

            const signedInUser = await login(email.trim(), password);

            // Never trust the role selected in the URL.
            // Check the role returned by the backend.
            if (signedInUser.role !== expectedRole) {
                await logout();
                setError(
                    `This account is not registered as ${expectedRole}. ` +
                    "Please use the correct role login."
                );
                return;
            }

            const destination = location.state?.from || "/dashboard";
            navigate(destination, { replace: true });

        } catch (err) {
            setError(
                err?.message ||
                    "Unable to sign in right now. Please try again."
            );
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div className="login-page">
            <section className="login-brand-panel">
                <Link to="/" className="login-brand">
                    <span className="brand-mark">P</span>
                    PromiseProof
                </Link>

                <div className="login-brand-copy">
                    <span className="welcome-label">
                        EVIDENCE. REVIEW. ACCOUNTABILITY.
                    </span>
                    <h1>Make every public commitment traceable.</h1>
                    <p>
                        A structured platform for recording promises,
                        examining evidence and tracking verified progress.
                    </p>
                </div>

                <span className="login-brand-footer">
                    Independent and politically neutral.
                </span>
            </section>

            <section className="login-form-panel">
                <div className="login-form-box">
                    <Link to="/" className="back-link">
                        ← Back to welcome page
                    </Link>

                    <div className="login-role-icon">
                        {details.icon}
                    </div>

                    <span className="welcome-label">
                        SECURE WORKSPACE ACCESS
                    </span>

                    <h2>{details.title}</h2>
                    <p className="login-description">
                        {details.description}
                    </p>

                    <form onSubmit={handleSubmit}>
                        <label htmlFor="email">Email address</label>
                        <input
                            id="email"
                            type="email"
                            value={email}
                            onChange={event => setEmail(event.target.value)}
                            placeholder="Enter your registered email"
                            autoComplete="username"
                            required
                        />

                        <label htmlFor="password">Password</label>
                        <input
                            id="password"
                            type="password"
                            value={password}
                            onChange={event => setPassword(event.target.value)}
                            placeholder="Enter your password"
                            autoComplete="current-password"
                            required
                        />

                        {error && (
                            <div className="login-error" role="alert">
                                {error}
                            </div>
                        )}

                        <button
                            type="submit"
                            className="login-submit"
                            disabled={submitting}
                        >
                            {submitting ? "Verifying account..." : "Sign in securely"}
                        </button>
                    </form>

                    {expectedRole === "CITIZEN" && (
                        <p className="login-register">
                            New to PromiseProof?{" "}
                            <Link to="/register">Create a citizen account</Link>
                        </p>
                    )}

                    {expectedRole !== "CITIZEN" && (
                        <p className="login-register">
                            This workspace is for accounts assigned by an
                            administrator. Public registration is not available
                            for this role.
                        </p>
                    )}

                    <div className="login-role-switch">
                        <span>Use another workspace</span>
                        <div>
                            <Link to="/login/citizen">Citizen</Link>
                            <Link to="/login/researcher">Researcher</Link>
                            <Link to="/login/reviewer">Reviewer</Link>
                            <Link to="/login/admin">Admin</Link>
                        </div>
                    </div>
                </div>
            </section>
        </div>
    );
}

export default Login;
