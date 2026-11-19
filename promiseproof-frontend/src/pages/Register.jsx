import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { useAuth } from "../auth/useAuth";

function Register() {

    const { register } = useAuth();
    const navigate = useNavigate();

    const [form, setForm] = useState({
        name: "",
        email: "",
        password: "",
        confirmPassword: ""
    });

    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    function handleChange(event) {

        const { name, value } = event.target;

        setForm(previous => ({
            ...previous,
            [name]: value
        }));
    }

    async function handleSubmit(event) {

        event.preventDefault();

        if (form.password !== form.confirmPassword) {
            setError("Passwords do not match");
            return;
        }

        try {
            setSubmitting(true);
            setError("");

            await register({
                name: form.name.trim(),
                email: form.email.trim(),
                password: form.password
            });

            navigate("/dashboard", { replace: true });

        } catch (err) {
            setError(
                err?.message ||
                    "Unable to create your account right now. " +
                        "Please try again."
            );
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <section className="auth-card">

            <div className="auth-header">
                <p className="eyebrow">JOIN THE PLATFORM</p>
                <h2>Create a citizen account</h2>
                <p>
                    Citizens can submit supporting evidence and track
                    their submissions. New accounts start with the
                    Citizen role.
                </p>
            </div>

            <form className="auth-form" onSubmit={handleSubmit}>

                <div className="form-group">
                    <label>Full Name</label>
                    <input
                        type="text"
                        name="name"
                        value={form.name}
                        onChange={handleChange}
                        placeholder="Your full name"
                        maxLength={120}
                        required
                    />
                </div>

                <div className="form-group">
                    <label>Email</label>
                    <input
                        type="email"
                        name="email"
                        value={form.email}
                        onChange={handleChange}
                        placeholder="you@example.com"
                        autoComplete="email"
                        required
                    />
                </div>

                <div className="form-group">
                    <label>Password</label>
                    <input
                        type="password"
                        name="password"
                        value={form.password}
                        onChange={handleChange}
                        placeholder="At least 8 characters"
                        minLength={8}
                        maxLength={72}
                        autoComplete="new-password"
                        required
                    />
                </div>

                <div className="form-group">
                    <label>Confirm Password</label>
                    <input
                        type="password"
                        name="confirmPassword"
                        value={form.confirmPassword}
                        onChange={handleChange}
                        placeholder="Repeat your password"
                        minLength={8}
                        maxLength={72}
                        autoComplete="new-password"
                        required
                    />
                </div>

                {error && (
                    <p className="error-message">{error}</p>
                )}

                <div className="auth-actions">
                    <button
                        type="submit"
                        className="primary-button"
                        disabled={submitting}
                    >
                        {submitting ? "Creating account..." : "Create Account"}
                    </button>

                    <Link
                        className="cancel-button auth-link-button"
                        to="/login"
                    >
                        I already have an account
                    </Link>
                </div>

            </form>

        </section>
    );
}

export default Register;
