/**
 * Usage: router.get('/admin/users', authRequired, requireRole('admin'), handler)
 */
function requireRole(...roles) {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthenticated' });
    }
    if (!roles.includes(req.user.role)) {
      return res.status(403).json({
        error: `Forbidden: requires role ${roles.join(' or ')}`
      });
    }
    next();
  };
}

module.exports = { requireRole };