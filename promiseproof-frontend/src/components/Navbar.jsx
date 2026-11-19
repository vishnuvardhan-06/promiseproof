import { Link, NavLink, useNavigate } from "react-router-dom";

import { useAuth } from "../auth/useAuth";
import NotificationBell from "./NotificationBell";

function navClass({ isActive }) {
    return isActive ? "nav-tab active" : "nav-tab";
}

function Navbar() {

    const { user, logout, loading } = useAuth();
    const navigate = useNavigate();

    const canReview = Boolean(
        user && (user.role === "REVIEWER" || user.role === "ADMIN")
    );

    const isAdmin = Boolean(user && user.role === "ADMIN");
    const isCitizen = Boolean(user && user.role === "CITIZEN");

    async function handleLogout() {

        try {
            await logout();
        } finally {
            navigate("/");
        }
    }

    return (
        <header className="navbar">

            <Link to="/dashboard" className="brand">
                <span className="brand-icon">P</span>
                <span>PromiseProof</span>
            </Link>

            <nav className="nav-tabs" aria-label="Main navigation">

                <NavLink to="/" end className={navClass}>
                    Home
                </NavLink>

                <NavLink to="/promises" end className={navClass}>
                    Browse Promises
                </NavLink>

                {isCitizen && (
                    <Link to="/dashboard?tab=submissions" className="nav-tab">
                        My Evidence
                    </Link>
                )}

                {isCitizen && (
                    <Link to="/dashboard?tab=account" className="nav-tab">
                        My Account
                    </Link>
                )}

                <NavLink to="/parties" className={navClass}>
                    Parties
                </NavLink>

                <NavLink to="/elections" className={navClass}>
                    Elections
                </NavLink>

                {user && (
                    <NavLink to="/dashboard" className={navClass}>
                        Dashboard
                    </NavLink>
                )}

                {canReview && (
                    <NavLink to="/review" className={navClass}>
                        Review
                    </NavLink>
                )}

                {isAdmin && (
                    <NavLink to="/admin" className={navClass}>
                        Admin
                    </NavLink>
                )}

            </nav>

            <div className="nav-auth">

                {loading ? null : user ? (
                    <>
                        <NotificationBell />

                        <div className="nav-user">
                            <span className="nav-user-name">
                                {user.name}
                            </span>
                            <span className="role-chip">
                                {user.role}
                            </span>
                        </div>

                        <button
                            type="button"
                            className="nav-link"
                            onClick={handleLogout}
                        >
                            Logout
                        </button>
                    </>
                ) : (
                    <>
                        <Link className="nav-link" to="/login">
                            Sign in
                        </Link>

                        <Link
                            className="nav-link nav-link-primary"
                            to="/register"
                        >
                            Register
                        </Link>
                    </>
                )}

            </div>

        </header>
    );
}

export default Navbar;
